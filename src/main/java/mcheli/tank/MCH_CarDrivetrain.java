package mcheli.tank;

/** Server-owned gameplay engine, transmission and longitudinal tire state (20 Hz). */
public final class MCH_CarDrivetrain {
   public float throttle, rpm;
   public int gear = 1; // -1 reverse, 1..N forward; no shift ever clears vehicle momentum
   public boolean serviceBrake, handbrake, running;
   public double frontWheelSpeed, rearWheelSpeed; // tire surface speed, blocks/tick
   public float frontSlip, rearSlip;
   public boolean frontContact, rearContact;
   private boolean initialized;
   private int directionTicks, shiftTicks;
   private double torqueRatio = 3.0D, shiftFrom = 3.0D;
   private boolean forwardDrive, reverseDrive;

   public void clearInputs() {
      this.throttle = 0;
      this.serviceBrake = this.handbrake = this.running = false;
      this.forwardDrive = this.reverseDrive = false;
      this.directionTicks = 0;
   }

   public void updateControls(MCH_TankInfo info, boolean up, boolean down, boolean handbrake,
                              boolean active, double speed, double horizontalSpeed) {
      this.running = active;
      this.handbrake = active && handbrake;
      this.forwardDrive = this.reverseDrive = false;
      if(!active) {
         this.clearInputs();
      } else {
         boolean reverseRequest = down && !up && info.enableBack;
         boolean forwardRequest = up;
         boolean changeDirection = reverseRequest && this.gear > 0 || forwardRequest && this.gear < 0;
         if(changeDirection && Math.abs(speed) <= 0.025D && horizontalSpeed <= 0.06D) {
            if(++this.directionTicks >= 4) {
               this.shift(info, reverseRequest ? -1 : 1);
               this.directionTicks = 0;
            }
         } else {
            this.directionTicks = 0;
         }
         this.forwardDrive = forwardRequest && this.gear > 0 && speed >= -0.025D;
         this.reverseDrive = reverseRequest && this.gear < 0 && speed <= 0.025D;
         // S remains a service brake throughout the stop-to-reverse dwell.
         this.serviceBrake = down && (up || this.gear > 0 || speed > 0.025D) || up && !this.forwardDrive;
         float target = up || this.reverseDrive ? 1.0F : 0.0F;
         this.throttle += (target - this.throttle) * info.carThrottleResponse;
         if(this.throttle < 0.001F) this.throttle = 0;
         if(this.gear > info.carForwardGears) this.shift(info, info.carForwardGears);
         if(this.gear > 0 && this.shiftTicks == 0) {
            double band = Math.max(0.05D, info.speed) / info.carForwardGears;
            if(this.forwardDrive && this.gear < info.carForwardGears && speed > band * this.gear * 0.88D) {
               this.shift(info, this.gear + 1);
            } else if(this.gear > 1 && Math.abs(speed) <= 0.025D) {
               this.shift(info, 1);
            } else if(this.gear > 1 && speed < band * (this.gear - 1) * 0.65D) {
               this.shift(info, this.gear - 1);
            }
         }
      }
      double targetRatio = this.ratio(info, this.gear);
      if(this.shiftTicks > 0) {
         this.shiftTicks = Math.min(this.shiftTicks, info.carShiftTicks);
         --this.shiftTicks;
         double progress = 1.0D - (double)this.shiftTicks / info.carShiftTicks;
         this.torqueRatio = this.shiftFrom + (targetRatio - this.shiftFrom) * progress;
      } else {
         this.torqueRatio = targetRatio;
      }
      double wheelSpeed = info.driveType == MCH_TankInfo.DriveType.FWD ? Math.abs(this.frontWheelSpeed)
            : info.driveType == MCH_TankInfo.DriveType.RWD ? Math.abs(this.rearWheelSpeed)
            : Math.max(Math.abs(this.frontWheelSpeed), Math.abs(this.rearWheelSpeed));
      double gearSpeed = this.gear < 0 ? Math.max(0.05D, info.civilianCarReverseSpeed > 0 ? info.civilianCarReverseSpeed : info.speed)
            : Math.max(0.05D, info.speed * this.gear / info.carForwardGears);
      double rev = Math.min(1.0D, Math.max(this.throttle * 0.25D, wheelSpeed / gearSpeed * 0.95D));
      float targetRpm = active ? (float)(info.carIdleRpm + (info.carRedlineRpm - info.carIdleRpm) * rev) : 0;
      this.rpm += (targetRpm - this.rpm) * 0.3F;
      this.rpm = Math.min(info.carRedlineRpm, this.rpm);
   }

   private double ratio(MCH_TankInfo info, int gear) {
      return gear < 0 || info.carForwardGears == 1 ? 1.0D
            : 3.0D - 2.0D * (gear - 1) / (info.carForwardGears - 1);
   }

   private void shift(MCH_TankInfo info, int gear) {
      this.shiftFrom = this.torqueRatio;
      this.shiftTicks = info.carShiftTicks;
      this.gear = gear;
   }

   /** Integrate wheel inertia and contact forces; lateral grip keeps its existing priority. */
   public double acceleration(MCH_TankInfo info, MCH_WheelManager.CarContact contact,
                              double speed, double sideways, boolean canDrive) {
      if(!this.initialized) {
         this.frontWheelSpeed = this.rearWheelSpeed = speed;
         this.initialized = true;
      }
      this.frontContact = contact.front > 0;
      this.rearContact = contact.rear > 0;
      boolean front = info.driveType != MCH_TankInfo.DriveType.RWD;
      boolean rear = info.driveType != MCH_TankInfo.DriveType.FWD;
      int driven = (front ? contact.configuredFront : 0) + (rear ? contact.configuredRear : 0);
      double demand = 0;
      if(canDrive && this.running) {
         if(this.forwardDrive) demand = 0.1D * this.throttle * this.torqueRatio;
         if(this.reverseDrive) {
            // Preserve the former steady reverse demand and the configured acceleration factor.
            demand = -Math.min(0.1D, 0.0125D * info.throttleUpDown * info.throttleDownFactor) * this.throttle;
         }
         double wheelSpeed = front && rear ? Math.max(Math.abs(this.frontWheelSpeed), Math.abs(this.rearWheelSpeed))
               : Math.abs(front ? this.frontWheelSpeed : this.rearWheelSpeed);
         double redlineSpeed = this.gear < 0 ? Math.max(0.05D, info.civilianCarReverseSpeed > 0
               ? info.civilianCarReverseSpeed : info.speed) : Math.max(0.05D, info.speed * this.gear / info.carForwardGears);
         // Soft fuel cut above the gear's redline; wheel inertia carries motion through the cut.
         demand *= Math.max(0.0D, Math.min(1.0D, (1.15D - wheelSpeed / redlineSpeed) / 0.15D));
      }
      double frontDrive = front && driven > 0 ? demand * contact.configuredFront / driven : 0;
      double rearDrive = rear && driven > 0 ? demand * contact.configuredRear / driven : 0;
      double frontLimit = MCH_CarTireGrip.axleDriveLimit(sideways, info, contact.front, contact.total, info.frontTireSize);
      double rearLimit = MCH_CarTireGrip.axleDriveLimit(sideways, info, contact.rear, contact.total, info.rearTireSize);
      double frontBrake = contact.total > 0 && this.serviceBrake
            ? info.carServiceBrake * contact.configuredFront / contact.total : 0;
      double rearBrake = contact.total > 0 && this.serviceBrake
            ? info.carServiceBrake * contact.configuredRear / contact.total : 0;
      if(this.handbrake && contact.configuredRear > 0) rearBrake += info.carHandbrake;
      double frontForce = this.axle(true, speed, frontDrive, frontBrake, frontLimit, contact.configuredFront, contact.total);
      double rearForce = this.axle(false, speed, rearDrive, rearBrake, rearLimit, contact.configuredRear, contact.total);
      double force = frontForce + rearForce;
      // Brake-only reaction must stop at rest, never kick the body through zero.
      if(demand == 0 && (this.serviceBrake || this.handbrake)) {
         if(speed == 0 || force * speed > 0) force = 0;
         else force = Math.copySign(Math.min(Math.abs(force), Math.abs(speed)), force);
      }
      this.frontSlip = this.spinSlip(this.frontWheelSpeed, speed + force, front && canDrive && this.running);
      this.rearSlip = this.spinSlip(this.rearWheelSpeed, speed + force, rear && canDrive && this.running);
      return force;
   }

   private double axle(boolean front, double speed, double drive, double brake, double capacity, int count, int total) {
      if(count <= 0 || total <= 0) return 0;
      double inertia = 4.0D * total / count;
      double wheel = (front ? this.frontWheelSpeed : this.rearWheelSpeed) + drive * inertia;
      // Brake torque opposes wheel rotation, including drive torque at a standstill.
      double brakeReserve = Math.max(0, brake - Math.abs(wheel) / inertia);
      wheel = Math.copySign(Math.max(0, Math.abs(wheel) - brake * inertia), wheel);
      double request = (wheel - speed) / (inertia + 1.0D);
      if(wheel == 0 && brakeReserve > 0) {
         // Static brake torque can hold a stopped wheel against the road reaction.
         request = -(speed + Math.signum(speed) * brakeReserve * inertia) / (inertia + 1.0D);
      }
      double force = Math.max(-capacity, Math.min(capacity, request));
      wheel -= force * inertia;
      wheel = Math.copySign(Math.max(0, Math.abs(wheel) - brakeReserve * inertia), wheel);
      wheel = Math.max(-8.0D, Math.min(8.0D, wheel));
      if(front) this.frontWheelSpeed = wheel;
      else this.rearWheelSpeed = wheel;
      return force;
   }

   private float spinSlip(double wheel, double speed, boolean powered) {
      double excess = Math.abs(wheel) - Math.abs(speed);
      return powered && excess > 0.12D ? (float)Math.min(3.0D, excess / Math.max(0.1D, Math.abs(speed))) : 0;
   }

   /** Compact, changed-only DataWatcher payloads; no new control packet or client authority. */
   public int engineState() {
      return Math.round(this.rpm) | (Math.round(this.throttle * 255) << 14) | ((this.gear + 1) << 22)
            | (this.serviceBrake ? 1 << 26 : 0) | (this.handbrake ? 1 << 27 : 0) | (this.running ? 1 << 28 : 0);
   }

   public int axleState(boolean front) {
      double speed = front ? this.frontWheelSpeed : this.rearWheelSpeed;
      float slip = front ? this.frontSlip : this.rearSlip;
      boolean contact = front ? this.frontContact : this.rearContact;
      return (Math.round((float)speed * 1000) & 65535) | (Math.round(slip * 1000) << 16) | (contact ? 1 << 28 : 0);
   }

   public void readState(int engine, int front, int rear) {
      this.rpm += ((engine & 16383) - this.rpm) * 0.35F;
      this.throttle += (((engine >>> 14 & 255) / 255.0F) - this.throttle) * 0.4F;
      this.gear = (engine >>> 22 & 15) - 1;
      this.serviceBrake = (engine & 1 << 26) != 0;
      this.handbrake = (engine & 1 << 27) != 0;
      this.running = (engine & 1 << 28) != 0;
      this.frontWheelSpeed += ((short)(front & 65535) / 1000.0D - this.frontWheelSpeed) * 0.4D;
      this.rearWheelSpeed += ((short)(rear & 65535) / 1000.0D - this.rearWheelSpeed) * 0.4D;
      this.frontSlip = (front >>> 16 & 4095) / 1000.0F;
      this.rearSlip = (rear >>> 16 & 4095) / 1000.0F;
      this.frontContact = (front & 1 << 28) != 0;
      this.rearContact = (rear & 1 << 28) != 0;
   }
}
