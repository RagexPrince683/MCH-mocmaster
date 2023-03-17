package mcheli.plane;

import mcheli.MCH_Config;
import mcheli.MCH_MOD;
import mcheli.aircraft.MCH_AircraftInfo;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class MCP_PlaneInfo extends MCH_AircraftInfo {

	public MCP_ItemPlane item = null;
	public List nozzles = new ArrayList();
	public List rotorList = new ArrayList();
	public List wingList = new ArrayList();
	public boolean isEnableVtol = false;
	public boolean isDefaultVtol;
	public float vtolYaw = 0.3F;
	public float vtolPitch = 0.2F;
	public boolean isEnableAutoPilot = false;
	public boolean isVariableSweepWing = false;
	public float sweepWingSpeed;
	public boolean lookdown = false;
	public int maxAlt = 300;
	public int range = 200;

	public int maxRPM=0;
	public double propPitch=0;
	public double propDiameter=0;
	public double numProps=0;
	public double dragCoefficient=0;
	double mass=0; //kg
	
	//All areas are in m^2
	public double wingArea     = 0;
	public double frontalArea  = 0;
	public double aileronArea  = 0;
	public double elevatorArea = 0;
	
	public Item getItem() {
		return this.item;
	}

	public MCP_PlaneInfo(String name) {
		super(name);
		this.sweepWingSpeed = super.speed;
	}

	public float getDefaultRotorSpeed() {
		return 47.94F;
	}

	private float getDefaultStepHeight() {
		return 0.6F;
	}

	public boolean haveNozzle() {
		return this.nozzles.size() > 0;
	}

	public boolean haveRotor() {
		return this.rotorList.size() > 0;
	}

	public boolean haveWing() {
		return this.wingList.size() > 0;
	}

	public float getMaxSpeed() {
		return 1.8F;
	}

	public int getDefaultMaxZoom() {
		return 8;
	}

	public String getDefaultHudName(int seatId) {
		return seatId <= 0?"plane":(seatId == 1?"plane":"gunner");
	}

	public boolean isValidData() throws Exception {
		if(this.haveHatch() && this.haveWing()) {
			this.wingList.clear();
			super.hatchList.clear();
		}

		double var10001 = (double)super.speed;
		MCH_Config var10002 = MCH_MOD.config;
		super.speed = (float)(var10001 * MCH_Config.AllPlaneSpeed.prmDouble);
		var10001 = (double)this.sweepWingSpeed;
		var10002 = MCH_MOD.config;
		this.sweepWingSpeed = (float)(var10001 * MCH_Config.AllPlaneSpeed.prmDouble);
		return super.isValidData();
	}

	public void loadItemData(String item, String data) {
		super.loadItemData(item, data);
		String[] s;
		if(item.compareTo("addpartrotor") == 0) {
			s = data.split("\\s*,\\s*");
			if(s.length >= 6) {
				float n = s.length >= 7?this.toFloat(s[6], -180.0F, 180.0F) / 90.0F:1.0F;
				MCP_PlaneInfo.Rotor n1 = new MCP_PlaneInfo.Rotor(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), n, "rotor" + this.rotorList.size());
				this.rotorList.add(n1);
			}
		}else if(item.equalsIgnoreCase("numProps")) {
			this.numProps = this.toInt(data);
		}else if(item.equalsIgnoreCase("maxRPM")) {
			this.maxRPM = this.toInt(data);
		}else if(item.equalsIgnoreCase("propPitch")) {
			this.propPitch = this.toDouble(data);
		}else if(item.equalsIgnoreCase("wingArea")) {
			this.wingArea = this.toDouble(data);
		}else if(item.equalsIgnoreCase("frontalArea")) {
			this.frontalArea = this.toDouble(data);
		}else if(item.equalsIgnoreCase("propDiameter")) {
			this.propDiameter = this.toDouble(data);
		}else if(item.equalsIgnoreCase("mass")) {
			this.mass = this.toDouble(data);
		}else if(item.equalsIgnoreCase("drag")) {
			this.dragCoefficient = this.toDouble(data);
		}else if(item.compareTo("alt") == 0) {
			this.maxAlt = this.toInt(data);
		} else if(item.compareTo("addblade") == 0) {
			int s1 = this.rotorList.size() - 1;
			MCP_PlaneInfo.Rotor n2 = this.rotorList.size() > 0?(MCP_PlaneInfo.Rotor)this.rotorList.get(s1):null;
			if(n2 != null) {
				String[] n4 = data.split("\\s*,\\s*");
				if(n4.length == 8) {
					MCP_PlaneInfo.Blade b = new MCP_PlaneInfo.Blade(this.toInt(n4[0]), this.toInt(n4[1]), this.toFloat(n4[2]), this.toFloat(n4[3]), this.toFloat(n4[4]), this.toFloat(n4[5]), this.toFloat(n4[6]), this.toFloat(n4[7]), "blade" + s1);
					n2.blades.add(b);
				}
			}
		} else {
			MCP_PlaneInfo.Wing n3;
			if(item.compareTo("addpartwing") == 0) {
				s = data.split("\\s*,\\s*");
				if(s.length == 7) {
					n3 = new MCP_PlaneInfo.Wing(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), "wing" + this.wingList.size());
					this.wingList.add(n3);
				}
			} else if(item.equalsIgnoreCase("AddPartPylon")) {
				s = data.split("\\s*,\\s*");
				if(s.length >= 7 && this.wingList.size() > 0) {
					n3 = (MCP_PlaneInfo.Wing)this.wingList.get(this.wingList.size() - 1);
					if(n3.pylonList == null) {
						n3.pylonList = new ArrayList();
					}

					MCP_PlaneInfo.Pylon n6 = new MCP_PlaneInfo.Pylon(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), this.toFloat(s[6]), n3.modelName + "_pylon" + n3.pylonList.size());
					n3.pylonList.add(n6);
				}
			} else if(item.compareTo("addpartnozzle") == 0) {
				s = data.split("\\s*,\\s*");
				if(s.length == 6) {
					MCH_AircraftInfo.DrawnPart n5 = new MCH_AircraftInfo.DrawnPart(this.toFloat(s[0]), this.toFloat(s[1]), this.toFloat(s[2]), this.toFloat(s[3]), this.toFloat(s[4]), this.toFloat(s[5]), "nozzle" + this.nozzles.size());
					this.nozzles.add(n5);
				}
			} else if(item.compareTo("variablesweepwing") == 0) {
				this.isVariableSweepWing = this.toBool(data);
			} else if(item.compareTo("sweepwingspeed") == 0) {
				this.sweepWingSpeed = this.toFloat(data, 0.0F, 5.0F);
			} else if(item.compareTo("enablevtol") == 0) {
				this.isEnableVtol = this.toBool(data);
			} else if(item.compareTo("defaultvtol") == 0) {
				this.isDefaultVtol = this.toBool(data);
			} else if(item.compareTo("vtolyaw") == 0) {
				this.vtolYaw = this.toFloat(data, 0.0F, 1.0F);
			} else if(item.compareTo("vtolpitch") == 0) {
				this.vtolPitch = this.toFloat(data, 0.01F, 1.0F);
			} else if(item.compareTo("enableautopilot") == 0) {
				this.isEnableAutoPilot = this.toBool(data);
			}
		}

	}

	public String getDirectoryName() {
		return "planes";
	}

	public String getKindName() {
		return "plane";
	}

	public void preReload() {
		super.preReload();
		this.nozzles.clear();
		this.rotorList.clear();
		this.wingList.clear();
	}

	public void postReload() {
		MCH_MOD.proxy.registerModelsPlane(super.name, true);
	}

	public class Rotor extends MCH_AircraftInfo.DrawnPart {

		public List blades = new ArrayList();
		public final float maxRotFactor;


		public Rotor(float x, float y, float z, float rx, float ry, float rz, float mrf, String model) {
			super(x, y, z, rx, ry, rz, model);
			this.maxRotFactor = mrf;
		}
	}

	public class Pylon extends MCH_AircraftInfo.DrawnPart {

		public final float maxRotFactor;
		public final float maxRot;


		public Pylon(float px, float py, float pz, float rx, float ry, float rz, float mr, String name) {
			super(px, py, pz, rx, ry, rz, name);
			this.maxRot = mr;
			this.maxRotFactor = this.maxRot / 90.0F;
		}
	}

	public class Blade extends MCH_AircraftInfo.DrawnPart {

		public final int numBlade;
		public final int rotBlade;


		public Blade(int num, int r, float px, float py, float pz, float rx, float ry, float rz, String name) {
			super(px, py, pz, rx, ry, rz, name);
			this.numBlade = num;
			this.rotBlade = r;
		}
	}

	public class Wing extends MCH_AircraftInfo.DrawnPart {

		public final float maxRotFactor;
		public final float maxRot;
		public List pylonList;


		public Wing(float px, float py, float pz, float rx, float ry, float rz, float mr, String name) {
			super(px, py, pz, rx, ry, rz, name);
			this.maxRot = mr;
			this.maxRotFactor = this.maxRot / 90.0F;
			this.pylonList = null;
		}
	}
}
