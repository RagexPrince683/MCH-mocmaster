package mcheli.sensors;

public class MCH_VisualContact {
    public int updated = 0;

    public double x,y,z;
    public double rotX, rotY, rotZ;
    public String model;
    public String texture;
    public int entityId;
    public int brightness;

    public MCH_VisualContact(double x, double y, double z, double rotX, double rotY, double rotZ, String model, String texture, int entityId) {
        //System.out.println("MCH_VisualContact");
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.model = model;
        this.texture = texture;
        this.entityId = entityId;
    }
}