package mcheli.aircraft;


import mcheli.tank.MCH_EntityTank;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

public class WheelBoundingBox extends MCH_BoundingBox {
    @Override
    public String getType() {
        return "wheel";
    }
    private int health;
    private MCH_EntityTank parentTank;
    public void setParentTank(MCH_EntityTank tank) {
        this.parentTank = tank;
    }

    public WheelBoundingBox(double x, double y, double z, float w, float h, float df, int initialHealth) {
        super(x, y, z, w, h, df);
        this.health = initialHealth;
    }

    //public static String getType() {
    //    return "wheel";
    //}

    /**
     * Reduces health by the specified amount. Removes the bounding box if health <= 0.
     */
    public void takeDamage(int damage) {
        System.out.println("takedamage " + damage);
        this.health -= damage;
        if (this.health <= 0) {
            System.out.println("destroyed");
            this.destroy();
        }
    }

    //public WheelBoundingBox copy() {
    //    return new WheelBoundingBox(this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor, this.health);
    //}
    //pray

    @Override
    public MCH_BoundingBox copy() {
        // Copy the original data and retain the health attribute
        WheelBoundingBox copy = new WheelBoundingBox(
                this.offsetX, this.offsetY, this.offsetZ, this.width, this.height, this.damegeFactor, this.health
        );
        return copy;
    }



    /**
     * Logic to handle destruction of the bounding box.
     */
    private void destroy() {
        System.out.println("a wheel was destroyed");
        if (this.parentTank != null) {
            this.parentTank.mobile = false;
        }
        // Custom logic to remove the bounding box
        // For example, flagging it for removal in the extraBoundingBox list
        this.boundingBox.setBounds(0, 0, 0, 0, 0, 0); // Effectively "removes" it
    }

    /**
     * Returns the current health of the bounding box.
     */
    public int getHealth() {
        return this.health;
    }

}
