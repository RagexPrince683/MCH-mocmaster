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
   private double torqueRatio, shiftFrom;
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
         // Released throttle decays through the same pedal response; an opposing pedal cuts drive.
         this.forwardDrive = !reverseRequest && this.gear > 0 && speed >= -0.025D;
         this.reverseDrive = !up && this.gear < 0 && speed <= 0.025D;
         // S remains a service brake throughout the stop-to-reverse dwell.
         this.serviceBrake = down && (up || this.gear > 0 || speed > 0.025D) || up && !this.forwardDrive;
         float target = up || reverseRequest && this.reverseDrive ? 1.0F : 0.0F;
         this.throttle += (target - this.throttle) * info.carThrottleResponse;
         if(this.throttle < 0.001F) this.throttle = 0;
         if(this.gear > info.carForwardGears) this.shift(info, info.carForwardGears);
         if(this.gear > 0 && this.shiftTicks == 0) {
            // Road RPM chooses gears; spinning/airborne tires rev the engine without racing through gears.
            double roadRpm = this.wheelRpm(info, Math.abs(speed), this.ratio(info, this.gear));
            if(up && !down && !handbrake && this.forwardDrive && this.gear < info.carForwardGears
                  && roadRpm >= info.carRedlineRpm * 0.88D) {
               this.shift(info, this.gear + 1);
            } else if(this.gear > 1 && Math.abs(speed) <= 0.025D) {
               this.shift(info, 1);
            } else if(this.gear > 1 && roadRpm < info.carRedlineRpm * 0.30D
                  && this.wheelRpm(info, Math.abs(speed), this.ratio(info, this.gear - 1)) < info.carRedlineRpm * 0.70D) {
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
   }

   private double drivenWheelSpeed(MCH_TankInfo info) {
      return info.driveType == MCH_TankInfo.DriveType.FWD ? Math.abs(this.frontWheelSpeed)
            : info.driveType == MCH_TankInfo.DriveType.RWD ? Math.abs(this.rearWheelSpeed)
            : Math.max(Math.abs(this.frontWheelSpeed), Math.abs(this.rearWheelSpeed));
   }

   private double wheelRpm(MCH_TankInfo info, double wheelSpeed, double ratio) {
      // blocks/tick -> metres/minute at 20 Hz; radius is independent of visual wheel scale.
      return wheelSpeed * 1200.0D / (2.0D * Math.PI * info.carWheelRadius) * ratio * info.carFinalDrive;
   }

   private void updateRpm(MCH_TankInfo info) {
      double coupled = this.wheelRpm(info, this.drivenWheelSpeed(info), this.torqueRatio);
      // Gameplay slipping launch clutch/torque converter permits revs against held brakes at rest.
      double launch = info.carIdleRpm + (info.carRedlineRpm - info.carIdleRpm) * this.throttle * 0.4D;
      float targetRpm = this.running ? (float)Math.min(info.carRedlineRpm, Math.max(launch, coupled)) : 0;
      this.rpm += (targetRpm - this.rpm) * 0.3F;
      this.rpm = Math.min(info.carRedlineRpm, this.rpm);
   }

   private double ratio(MCH_TankInfo info, int gear) {
      return gear < 0 ? info.carReverseGearRatio : info.carGearRatios[Math.max(0, Math.min(info.carForwardGears - 1, gear - 1))];
   }

   private void shift(MCH_TankInfo info, int gear) {
      this.shiftFrom = this.torqueRatio > 0 ? this.torqueRatio : this.ratio(info, this.gear);
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
         double engineForce = info.carDriveForce * this.torqueRatio * info.carFinalDrive * this.throttle;
         double rev = Math.max(info.carIdleRpm, this.rpm) / info.carRedlineRpm;
         // Bounded gameplay torque curve: broad midrange peak, falling torque near redline.
         engineForce *= 0.7D + 0.3D * Math.min(1.0D, rev / 0.5D) - 0.25D * Math.max(0, (rev - 0.5D) / 0.5D);
         if(this.shiftTicks > 0) engineForce *= 0.25D + 0.75D * (1.0D - (double)this.shiftTicks / info.carShiftTicks);
         if(this.forwardDrive) demand = engineForce;
         if(this.reverseDrive) {
            // Retain reverse acceleration tuning at this boundary; reverse speed has its own ceiling.
            demand = -engineForce * info.throttleUpDown * info.throttleDownFactor / 2.4D;
         }
         double wheelSpeed = this.drivenWheelSpeed(info);
         double wheelRpm = this.wheelRpm(info, wheelSpeed, this.torqueRatio);
         demand *= Math.max(0.0D, Math.min(1.0D, (1.03D - wheelRpm / info.carRedlineRpm) / 0.08D));
         if(this.gear < 0 && info.civilianCarReverseSpeed > 0) {
            demand *= Math.max(0, Math.min(1, (1.10D - wheelSpeed / info.civilianCarReverseSpeed) / 0.10D));
         }
      }
      double frontDrive = front && driven > 0 ? demand * contact.configuredFront / driven : 0;
      double rearDrive = rear && driven > 0 ? demand * contact.configuredRear / driven : 0;
      double frontLimit = MCH_CarTireGrip.axleDriveLimit(sideways, info, contact.front, contact.total, info.frontTireSize);
      double rearLimit = MCH_CarTireGrip.axleDriveLimit(sideways, info, contact.rear, contact.total, info.rearTireSize);
      // Use the existing contact/lateral reservation with a car-scale longitudinal traction budget.
      frontLimit *= info.carLongitudinalGrip / 0.24D;
      rearLimit *= info.carLongitudinalGrip / 0.24D;
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
      } else if((this.serviceBrake || this.handbrake) && force * speed < 0) {
         // Combined pedals may restrain a launch, but brake reaction cannot reverse travel.
         force = Math.copySign(Math.min(Math.abs(force), Math.abs(speed)), force);
      }
      this.frontSlip = this.spinSlip(this.frontWheelSpeed, speed + force, front && canDrive && this.running);
      this.rearSlip = this.spinSlip(this.rearWheelSpeed, speed + force, rear && canDrive && this.running);
      this.updateRpm(info);
      return force;
   }

   /** Explicit quadratic air drag; tiny contact rolling resistance, no speed-target servo. */
   public double drag(MCH_TankInfo info, double speed, boolean supported) {
      return Math.min(speed, info.carDrag * speed * speed + (supported ? 0.00015D : 0.0D));
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
