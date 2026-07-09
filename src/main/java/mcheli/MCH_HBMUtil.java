package mcheli;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

//Could literally just check for the HBM mod being installed and not do reflection.
// But people who steal mods & copy shit coding standards don't actually care about clean coding.
//TODO: change slop
public class MCH_HBMUtil {
    private static Class<?> nukeExplosionMK5Class;
    private static Class<?> nukeTorexClass;
    private static Class<?> explosionChaosClass;
    private static Class<?> explosionCreatorClass;
    private static Class<?> explosionNT;

    static {
        try {
            nukeExplosionMK5Class = Class.forName("com.hbm.entity.logic.EntityNukeExplosionMK5");
            nukeTorexClass = Class.forName("com.hbm.entity.effect.EntityNukeTorex");
            explosionChaosClass = Class.forName("com.hbm.explosion.ExplosionChaos");
            explosionCreatorClass = Class.forName("com.hbm.particle.helper.ExplosionCreator");
            explosionNT = Class.forName("com.hbm.explosion.ExplosionNT");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static final String HBM_ENABLE_NUKES_COMMAND = "ntmenablenukes";
    private static boolean mcheliNukesEnabled = true;

    public static boolean areNukesEnabled() {
        Boolean hbmEnabled = getHBMNukesEnabled();
        if (hbmEnabled != null) {
            mcheliNukesEnabled = hbmEnabled.booleanValue();
        }
        return mcheliNukesEnabled;
    }

    public static void setMCHeliNukesEnabled(boolean enabled) {
        mcheliNukesEnabled = enabled;
        setHBMNukesEnabled(enabled);
    }

    public static boolean hasHBMEnableNukesCommand() {
        return getHBMEnableNukesCommand() != null;
    }

    public static void syncHBMEnableNukesCommand(net.minecraft.command.ICommandSender sender, boolean enabled) {
        try {
            net.minecraft.command.ICommand command = getHBMEnableNukesCommand();
            if (command != null) {
                command.processCommand(sender, new String[]{String.valueOf(enabled)});
                setHBMNukesEnabled(enabled);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static net.minecraft.command.ICommand getHBMEnableNukesCommand() {
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            if (server != null && server.getCommandManager() != null) {
                java.lang.reflect.Method getCommands = server.getCommandManager().getClass().getMethod("getCommands", new Class[0]);
                Object commands = getCommands.invoke(server.getCommandManager(), new Object[0]);
                if (commands instanceof java.util.Map) {
                    Object command = ((java.util.Map)commands).get(HBM_ENABLE_NUKES_COMMAND);
                    if (command instanceof net.minecraft.command.ICommand) {
                        return (net.minecraft.command.ICommand)command;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Boolean getHBMNukesEnabled() {
        Object command = getHBMEnableNukesCommand();
        Boolean enabled = getBooleanField(command, "enableNukes", "enabled", "nukes", "allowNukes");
        if (enabled != null) {
            return enabled;
        }
        return getBooleanFieldFromClasses(new String[]{"com.hbm.config.BombConfig", "com.hbm.config.GeneralConfig", "com.hbm.config.WeaponConfig"}, new String[]{"enableNukes", "enabledNukes", "nukes", "allowNukes"});
    }

    private static void setHBMNukesEnabled(boolean enabled) {
        Object command = getHBMEnableNukesCommand();
        setBooleanField(command, enabled, "enableNukes", "enabled", "nukes", "allowNukes");
        setBooleanFieldInClasses(new String[]{"com.hbm.config.BombConfig", "com.hbm.config.GeneralConfig", "com.hbm.config.WeaponConfig"}, enabled, new String[]{"enableNukes", "enabledNukes", "nukes", "allowNukes"});
    }

    private static Boolean getBooleanField(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                if (field.getType() == Boolean.TYPE) {
                    return Boolean.valueOf(field.getBoolean(target));
                }
                if (field.getType() == Boolean.class) {
                    return (Boolean)field.get(target);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Boolean getBooleanFieldFromClasses(String[] classNames, String[] fieldNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                for (String fieldName : fieldNames) {
                    try {
                        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        if (field.getType() == Boolean.TYPE) {
                            return Boolean.valueOf(field.getBoolean(null));
                        }
                        if (field.getType() == Boolean.class) {
                            return (Boolean)field.get(null);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void setBooleanField(Object target, boolean value, String... names) {
        if (target == null) {
            return;
        }
        for (String name : names) {
            try {
                java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                if (field.getType() == Boolean.TYPE) {
                    field.setBoolean(target, value);
                } else if (field.getType() == Boolean.class) {
                    field.set(target, Boolean.valueOf(value));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void setBooleanFieldInClasses(String[] classNames, boolean value, String[] fieldNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                for (String fieldName : fieldNames) {
                    try {
                        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        if (field.getType() == Boolean.TYPE) {
                            field.setBoolean(null, value);
                        } else if (field.getType() == Boolean.class) {
                            field.set(null, Boolean.valueOf(value));
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }


    public static Object EntityNukeExplosionMK5_statFac(World world, int r, double posX, double posY, double posZ) {
        try {
            if (nukeExplosionMK5Class != null) {
                Method statFacMethod = nukeExplosionMK5Class.getMethod("statFac", World.class, int.class, double.class, double.class, double.class);
                return statFacMethod.invoke(null, world, r, posX, posY, posZ);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void EntityNukeTorex_statFac(World world, double posX, double posY, double posZ, float nukeYield) {
        try {
            if (nukeTorexClass != null) {
                Method statFacMethod = nukeTorexClass.getMethod("statFac", World.class, double.class, double.class, double.class, float.class);
                statFacMethod.invoke(null, world, posX, posY, posZ, nukeYield);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ExplosionChaos_spawnChlorine(World world, double posX, double posY, double posZ, float chemYield, double chemSpeed, int chemType) {
        try {
            System.out.println("spawn chlorine method");
            if (explosionChaosClass != null) {
                Method spawnChlorineMethod = explosionChaosClass.getMethod("spawnChlorine",
                        World.class, double.class, double.class, double.class, int.class, double.class, int.class);
                spawnChlorineMethod.invoke(null, world, posX, posY, posZ, (int)chemYield, chemSpeed, chemType);
                System.out.println("Chlorine effect spawned successfully");
                //ExplosionChaos.spawnChlorine(worldObj, posX, posY, posZ, 50, 1.5, 0);

            }
        } catch (Exception e) {
            System.out.println("Failed to spawn chlorine effect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ExplosionCreator_composeEffectStandard(World world, double posX, double posY, double posZ, int explosionBlockSize) {
        try {
            if (explosionCreatorClass != null) {
                Method spawnChlorineMethod;
                if(explosionBlockSize<50) {
                    spawnChlorineMethod = explosionCreatorClass.getMethod("composeEffectSmall", World.class, double.class, double.class, double.class);
                } else if (explosionBlockSize<100) {
                    spawnChlorineMethod = explosionCreatorClass.getMethod("composeEffectStandard", World.class, double.class, double.class, double.class);
                }
                else {
                    spawnChlorineMethod = explosionCreatorClass.getMethod("composeEffectLarge", World.class, double.class, double.class, double.class);
                }
                spawnChlorineMethod.invoke(null, world, posX, posY, posZ);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object ExplosionNT_instance_init(World world, Entity entity, double posX, double posY, double posZ, float explosionPower) {
        try {
            if (explosionNT != null) {
                Class<?>[] explosionNTParamTypes = {World.class, Entity.class, double.class, double.class, double.class, float.class};
                Constructor<?> explosionNTConstructor = explosionNT.getConstructor(explosionNTParamTypes);
                return explosionNTConstructor.newInstance(world, entity, posX, posY, posZ, explosionPower);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ExplosionNT_instance_overrideResolutionAndExplode(Object explosionNTInstance, int resolution) {
        try {
            if (explosionNTInstance != null) {
                Method overrideResolutionMethod = explosionNTInstance.getClass().getMethod("overrideResolution", int.class);
                overrideResolutionMethod.invoke(explosionNTInstance, resolution);
                Method explodeMethod = explosionNTInstance.getClass().getMethod("explode");
                explodeMethod.invoke(explosionNTInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void ExplosionNT_instance_addAttrib(Object explosionNTInstance, String attrib) {
        try {
            if (explosionNTInstance != null) {
                Class<?> exAttribClass = Class.forName("com.hbm.explosion.ExplosionNT$ExAttrib");
                Object Attrib = Enum.valueOf((Class<Enum>) exAttribClass, attrib);
                Method addAttribMethod = explosionNT.getMethod("addAttrib", exAttribClass);
                addAttribMethod.invoke(explosionNTInstance,Attrib);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
