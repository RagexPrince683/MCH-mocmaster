MC Heli MOD [SSP/SMP]

com.norwood.mcheli.cfg �̊e���ڂɂ��Đ������L�ڂ���B
�}���`�̏ꍇ�A�����̍��ڂ̓T�[�o���̐ݒ肪�D�悳���B

�������̍��ڂ͋@�̂�R�L�[�ŊJ���⋋��ʂ�MOD Option����N�������܂ܕύX�\�ł���B
�Ώۂ̐ݒ�ɂ́��}�[�N��t�����B

�܂��A�V���O���v���C���}���`��OP����������΁A/mcheli reconfig �R�}���h�ŋN�������܂�
com.norwood.mcheli.cfg ���ēǂݍ��݂ł���B

--------------------------------------------------------------------------------------------------------------------

Q : �u���b�N�̔j��⒅�΂�h���ɂ́H
A : Explosion_DestroyBlock, Explosion_FlamingBlock, Collision_DestroyBlock �� false �ɐݒ肵�Ă��������B
�@�@����� BulletBreakableBlocks �� - �ɂ��Ă��������B

Q : �q��@���炾�ƃ��u�⃂���X�^�[���`��͈͊O�ŕ\������Ȃ�
A : MobRenderDistanceWeight �� 3.00 ���炢�ɂ��Ă��������B�`�拗����3�{�ɐL�т܂��B

Q : �R���A�e��𖳌��ɂ�����
A : InfinityAmmo, InfinityFuel �� true �ɂ��Ă��������B

Q : �}���`�v���C�����ɋ@�̂̐ݒu�ꏊ�𐧌�������
A : PlaceableOnSpongeOnly �� true �ɂ���ƃX�|���W�̏�݂̂ɐݒu�ł���悤�ɂȂ�܂��B


--------------------------------------------------------------------------------------------------------------------

TestMode = false
	���f���J���җp�̐ݒ�ŁAtrue �ɂ���ƃ��f���̓����蔻�����Ȃ̈ʒu���\�������B
	�Q�[�����N�����ł��@�̂�R�L�[�ŊJ���⋋��ʂ� MOD Option ����ύX�\�B
	��MOD option > Test Mode
EnableCommand = true
	�w��MOD�p�̃R�}���h(/mcheli ???) �̗L�������ݒ�Afalse �ɂ���Ǝg�p�ł��Ȃ��Ȃ�B

PlaceableOnSpongeOnly = false
	true �ɂ���Ƌ@�̂��X�|���W�̏�݂̂ɐݒu�ł���悤�ɂ���B�}���`�ł̐ݒu�����p�B
ItemDamage = true
	�@�̂��A�C�e���������Ƃ��_���[�W�������p�����ǂ����̐ݒ�Bfalse�ɂ���ƈ����p�����A�Đݒu��HP���񕜂���B
ItemFuel = true
	�@�̂��A�C�e���������Ƃ��R���������p�����ǂ����̐ݒ�Bfalse�ɂ���ƈ����p�����A�Đݒu�ŔR������ɂȂ�B
AutoRepairHP = 0.50
	�@�̂̎��R�񕜂��s��HP�̊����ŁA0.5��50%���Ӗ����A50%�ȏ�HP���c���Ă���ꍇ�������R�񕜂���B
	0.0�̏ꍇ��ɉ񕜂��A1.0�ȏ�̒l��ݒ肷��Ǝ��R�񕜂��Ȃ��Ȃ�B

Explosion_DestroyBlock = true
	�����ɂ��u���b�N�̔j��̗L�������ݒ�ŁAfalse�ɂ���ƃu���b�N�������ɂ���ĉ��Ȃ��Ȃ�B
Explosion_FlamingBlock = true
	�����ɂ��u���b�N�ւ̒��΂̗L�������ݒ�ŁAfalse�ɂ���ƃu���b�N�΂����Ȃ��Ȃ�B
BulletBreakableBlocks = glass_pane, stained_glass_pane, tallgrass, double_plant, yellow_flower, red_flower, vine, wheat, reeds, waterlily
	�e�e�����������ۂɔj�󂷂�u���b�N���B - �ɂ���ȂǁA���݂��Ȃ��u���b�N���̐g�ɂ���ƃu���b�N���j�󂳂�Ȃ��Ȃ�B
Collision_DestroyBlock = true
	�ԗ����u���b�N�ɂԂ������ۂɔj�󂷂邩�ǂ����̐ݒ�ŁAfalse �ɂ���ƃu���b�N���j�󂳂�Ȃ��Ȃ�B
Collision_Car_BreakableBlock = double_plant, glass_pane,stained_glass_pane
	��p�ԂȂǔ�r�I�y���ԗ����u���b�N�ɂԂ������ۂɔj�󂷂�u���b�N���B
Collision_Car_BreakableMaterial = cactus, cake, gourd, leaves, vine, plants
	��p�ԂȂǔ�r�I�y���ԗ����u���b�N�ɂԂ������ۂɔj�󂷂�u���b�N�̃}�e���A�����B
Collision_Tank_BreakableBlock = nether_brick_fence
	��ԂȂǏd���ԗ����u���b�N�ɂԂ������ۂɔj�󂷂�u���b�N���B
Collision_Tank_BreakableMaterial = cactus, cake, carpet, circuits, glass, gourd, leaves, vine, wood, plants
	��ԂȂǏd���ԗ����u���b�N�ɂԂ������ۂɔj�󂷂�u���b�N�̃}�e���A�����B
Collision_EntityDamage = true
	�ԗ����G���e�B�e�B�ɂԂ������ۂɃ_���[�W��^���邩�ǂ����̐ݒ�ŁAfalse�Ń_���[�W�𖳌����ł���
Collision_EntityTankDamage = false
	��ԓ��m���Ԃ������ۂɃ_���[�W��^���邩�ǂ����̐ݒ�ŁAtrue�Ő�Ԃɂ��_���[�W��^������悤�ɂȂ�

InfinityAmmo = false
	true �ɂ���ƒe�������ɂȂ�B
InfinityFuel = false
	true �ɂ���ƔR�����Ȃ��Ă����c�ł���B
DismountAll = false
	true �ɂ���Ƌ@�̂���~�肽�Ƃ��ɑ��̃��u���S���~�낷(�v���C���[������)�B
	
MountMinecartHeli = true
MountMinecartPlane = true
MountMinecartVehicle = false
MountMinecartTank = true
	true �ɂ���Ƌ@�̂��}�C���J�[�g�ɏ��B�w���R�v�^�[�A�Œ藃�@�A�n�㕺��A�ԗ��Ōʂɐݒ�ł���B
PreventingBroken = false
	true �ɂ���ƁA�ݒu������A�C�e�����ł��Ȃ��Ȃ�B
DropItemInCreativeMode = false
	true �ɂ���ƁA�N���G�C�e�B�u���[�h�ŋ@�̂����������ꍇ�ɃA�C�e��������B
BreakableOnlyPickaxe = false
	true �ɂ���ƁA��͂������ŃA�C�e�����ł���悤�ɂȂ�B

AllHeliSpeed = 1.00
AllPlaneSpeed = 1.00
AllTankSpeed = 1.00
	�S�@�̂̑��x�̔{���B1.0���傫������Ƒ����Ȃ�B
	1.0��菬��������ƒx���Ȃ�B

HurtResistantTime = 0.00
	���u�Ƀ_���[�W��^������̖��G���Ԃ̔{���B
	0.00 �͖��G���ԂȂ��B
StingerLockRange = 320.00
	�g�ѕ���̃��b�N�\�����B
	�`��͈͂𒴂����l���ݒ�ł��邪�A�`��͈͊O�̃G���e�B�e�B�̓��b�N�ł��Ȃ��B
RangeFinderSpotDist = 400
	�����W�t�@�C���_�[�̃X�|�b�g�ő勗���B
	�傫�Ȓl��ݒ肵�Ă��A�}�C�N�����̂̎d�l�Ő��������B
RangeFinderSpotTime = 15
	�����W�t�@�C���_�[�̃X�|�b�g�}�[�N�̕\������(�b)�B
RangeFinderConsume = true
	false �ɂ���ƃ����W�t�@�C���_�[���g�p���Ƀo�b�e��������Ȃ��Ȃ�B
EnablePutRackInFlying = true
	false �ɂ���ƁA��s���ɋ@�̂̐ςݍ��݂��ł��Ȃ��Ȃ�B
EnableDebugBoundingBox = true
	false �ɂ���ƁACtrl + B �ŕ\������铖���蔻��i�����l�p�j���\������Ȃ��Ȃ�B
	�}���`�v���C�ł̎g�p�𐧌��������ꍇ�Ɏg���B
DespawnCount = 25
	�@�̂��j�󂳂ꂽ�������܂ł̕b���B�}���`�v���C�ł͒Z������ƕ��׌y���ɂȂ�B
HitBoxDelayTick = 0
	�}���`�v���C�����̓����蔻��̒x��Tick�B
	�@�̂�n�ォ�猂�������Ɍ����ڂ�薾�炩�ɑO��_��Ȃ��Ɠ�����Ȃ��ꍇ
	�N���C�A���g���̕\�����x�����Ă��邽�߁A���̐ݒ�ŃT�[�o���̔����x�������邱�Ƃ��ł���B
	1���Z���邽�т� 1/20�b ���x�����Ă����B
EnableRotationLimit = true
	�w���R�v�^�[�̃s�b�`�A���[���ɐ�����������B
PitchLimitMax = 10
	EnableRotationLimit ��true�̏ꍇ�̃s�b�`�̏�����̐���
PitchLimitMin = -10
	EnableRotationLimit ��true�̏ꍇ�̃s�b�`�̉������̐���
RollLimit = 35
	EnableRotationLimit ��true�̏ꍇ�̃��[���̐���
RangeOfGunner_VsMonster_Horizontal = 80
	�΃����X�^�[�p�K���i�[�̐��������̍U���͈�
RangeOfGunner_VsMonster_Vertical = 160
	�΃����X�^�[�p�K���i�[�̐��������̍U���͈�
RangeOfGunner_VsPlayer_Horizontal = 200
	�Α��`�[���v���C���[�p�K���i�[�̐��������̍U���͈�
RangeOfGunner_VsPlayer_Vertical = 300
	�Α��`�[���v���C���[�p�K���i�[�̐��������̍U���͈�
FixVehicleAtPlacedPoint = true
	true �̏ꍇ�n�㕺�킪�ݒu�ʒu�ɌŒ肳��A�����Ȃǂňړ����Ȃ��Ȃ�
KillPassengersWhenDestroyed = false
	true �ɂ���Ƌ@�̂��j�󂳂ꂽ�Ƃ��ɏ���Ă��郂�u���S����_���[�W���󂯂�

--------------------------------------------------------------------------------------------------------------------

InvertMouse = false
	true �ɂ���ƁA�@�̑��c���̃}�E�X�̏㉺�𔽓]����B
	��MOD option > Invert Mouse
MouseSensitivity = 10.00
	�}�E�X���x�̔{���B
	��MOD option > Sensitivity
MouseControlStickModeHeli = false
MouseControlStickModePlane = false
	true �ɂ���ƁA���c�����f�t�H���g�̈ʒu�ɖ߂�Ȃ��Ȃ�B
	��MOD option > Stick Mode Heli
	��MOD option > Stick Mode Plane
;MouseControlFlightSimMode = true ( Yaw:key, Roll=mouse )
MouseControlFlightSimMode = false
	true �ɂ���ƁA�Œ藃�@�̂݃}�E�X�ł̑��삪�t���C�g�V�~�����[�^�ɋ߂��Ȃ�B
	��MOD option > Mouse Flight Sim Mode
AutoThrottleDownHeli = true
AutoThrottleDownPlane = false
AutoThrottleDownTank = false
	true�ɂ���ƁAW�L�[�������Ă��Ȃ��ƃX���b�g����������Bfalse �ɂ���ƃX���b�g���͎����ŉ�����Ȃ��B
	��MOD option > Throttle Down Heli
	��MOD option > Throttle Down Plane
	��MOD option > Throttle Down Tank
SwitchWeaponWithMouseWheel = true
	false �ɂ���ƁA�}�E�X�X�N���[���ɂ�镐��؂�ւ����ł��Ȃ��Ȃ�B
	��MOD option > Switch Weapon Wheel
	
LWeaponAutoFire = false
	true �ɂ���ƌg�ѕ���̂݃��b�N�I���������������_�Ŕ��˂����B

;DisableItemRender = 0 ~ 3 (1 = Recommended)
DisableItemRender = 1
	�@�̂ɏ�������̃A�C�e���\���𖳌�������ݒ�B
	0�ŕ\�������B��\���ɂ���ꍇ��1�ȍ~��ݒ肷��B
	�ق���MOD�Ƌ�������ꍇ��1�ȊO��ݒ肵�Ă݂�Ǝ��邱�Ƃ�����B
HideKeybind = false
	��ʂɕ\������鑀��L�[�̕\���������B
	��MOD option > Render Settings > Hide Key Binding
RenderDistanceWeight = 10.00
	�@�̂̕\�������̔{���B�傫������ƕ\���������L�т邪�A
	�}�b�v�����[�h����Ă���͈͂��炢�܂ł����L�тȂ��B
MobRenderDistanceWeight = 1.00
	���u�̕`�拗���̔{����L�΂��B�q��@�̏���Ă����
	���u�Ƃ̋������������ĕ\������Ȃ����Ƃ�����̂ŁA
	���̐ݒ�ŐL�΂��Ƃ悢�B2.00��2�{�B
	
CreativeTabIconItem = fuel
CreativeTabIconHeli = ah-64
CreativeTabIconPlane = f22a
CreativeTabIconTank = merkava_mk4
CreativeTabIconVehicle = mk15
	�N���G�C�e�B�u�^�u�̕\���A�C�e���B���D�݂ŁB
DisableShader = false
	true �ɂ���ƁA�J�������[�h�̔������[�h�������ɂȂ�B
DefaultExplosionParticle = false
	true �ɂ���ƁA�����G�t�F�N�g���o�j���̂��̂ɖ߂��B
	�w��MOD�̔����G�t�F�N�g���d���Ƃ��Ɏg�p����B
	��MOD option > Render Settings > Default Explosion
AliveTimeOfCartridge = 200
	���䰂̕\�����ԁB�d���ꍇ�͏��������邱�ƁB
;HitMarkColor = Alpha, Red, Green, Blue
HitMarkColor = 255, 255, 0, 0
	�G���e�B�e�B�Ƀ_���[�W��^�����Ƃ��̃q�b�g�}�[�N�̐F�B
	��MOD option > Render Settings > Red/Green/Blue/Alpha
SmoothShading = true
	false �ɂ���ƁA�@�̂̃��f�������炩�ɕ\������@�\�𖳌����ł���B
	�`�悪�d���ꍇ�Ɏg���B
	��MOD option > Render Settings > Smooth Shading
	
EnableModEntityRender = true
	false �ɂ���ƁA�@�̂ɏ�������u�̕`�揈�����ς��A
	�@�̂ɍ��킹�ČX�����Ƃ��ł��Ȃ��Ȃ�B
	�`��nMOD�Ƌ�������ꍇ�Ɏg�p����B
DisableRenderLivingSpecials = true
	false �ɂ���ƁA�����@�̂ɏ���Ă��郂�u�̖��O�\��������Ȃ��Ȃ�B
	�O�Ȃ̃v���C���[������Ȃ̃v���C���[�̎��E�ɓ����Ďז��ɂȂ�̂�h���ݒ�B
	�`��nMOD�Ƌ�������ꍇ�Ɏg�p����B
DisplayHUDThirdPerson = false
	true �ɂ���ƁA3�l�̎��_�ł�HUD���\�������B
	��MOD option > Render Settings > Show HUD Third Person
DisableThirdPersonCameraDistChange = false
	3�l�̎��_�ł̋����ύX�@�\(PageUp,PageDown�L�[)�𖳌�������B
	�}���`�Ő����������ꍇ�Ɏg�p����B
EnableReplaceTextureManager = true
	false �ɂ���ƁA�J�������[�h�̔������[�h���Ƀ��u�������Ȃ��Ȃ�B
	�`��nMOD�Ƌ�������ꍇ�Ɏg�p����B
DisplayEntityMarker = true
	false �ɂ���ƁA�����}�[�N/�X�|�b�g�}�[�N�̕\����OFF�ɂł���B
	�}���`�Ő����������ꍇ�Ɏg�p����B
	��MOD option > Render Settings > Show HUD Third Person
	���ӁF�}���`�v���C����MOD option��ʂ���ύX���Ă����f����Ȃ��B
	���f����ɂ̓T�[�o����com.norwood.mcheli.cfg��ύX���� /mcheli reconfig �R�}���h���g�p���邱�ƁB
EntityMarkerSize = 10.00
	�����}�[�N/�X�|�b�g�}�[�N�̕\���T�C�Y�B
	��MOD option > Render Settings > Entity Marker Size
BlockMarkerSize = 6.00
	�u���b�N�}�[�N�̕\���T�C�Y�B
	��MOD option > Render Settings > Block Marker Size
ReplaceRenderViewEntity = true
	false �ɂ���ƁA�J�����ʒu�̕ύX�@�\�������ɂȂ�A��Ƀv���C���[�ڐ��ɂȂ�B
	�`��nMOD�Ƌ�������ꍇ�Ɏg�p����B
	��MOD option > Render Settings > Change Camera Pos
	
--------------------------------------------------------------------------------------------------------------------

ItemRecipe_Fuel = "ICI", "III", I, iron_ingot, C, coal
ItemRecipe_GLTD = " B ", "IDI", "IRI", B, iron_block, I, iron_ingot, D, diamond, R, redstone
ItemRecipe_Chain = "I I", "III", "I I", I, iron_ingot
ItemRecipe_Parachute = "WWW", "S S", " W ", W, wool, S, string
ItemRecipe_Container = "CCI", C, chest, I, iron_ingot
ItemRecipe_UavStation = "III", "IDI", "IRI", I, iron_ingot, D, diamond, R, redstone_block
ItemRecipe_UavStation2 = "IDI", "IRI", I, iron_ingot, D, diamond, R, redstone
ItemRecipe_DraftingTable = "R  ", "PCP", "F F", R, redstone, C, crafting_table, P, planks, F, fence
ItemRecipe_Wrench = " I ", " II", "I  ", I, iron_ingot
ItemRecipe_RangeFinder = "III", "RGR", "III", I, iron_ingot, G, glass, R, redstone
ItemRecipe_Stinger = "G  ", "III", "RI ", G, glass, I, iron_ingot, R, redstone
ItemRecipe_StingerMissile = "R  ", " I ", "  G", G, gunpowder, I, iron_ingot, R, redstone
ItemRecipe_Javelin = "III", "GR ", G, glass, I, iron_ingot, R, redstone
ItemRecipe_JavelinMissile = " R ", " I ", " G ", G, gunpowder, I, iron_ingot, R, redstone

--------------------------------------------------------------------------------------------------------------------

DamageVsEntity = 1.0
	MCHeli�̋@�� �� �}�C���J�[�g�Ȃ�HeliMOD�ȊO�̃G���e�B�e�B �ɗ^����_���[�W�̔{���B
DamageVsLiving = 1.0
	MCHeli�̋@�� �� ���u �ɗ^����_���[�W�̔{���B
DamageVsPlayer = 1.0
	MCHeli�̋@�� �� �v���C���[ �ɗ^����_���[�W�̔{���B
	
DamageVsMCHeliAircraft = 1.0
	MCHeli�̋@�� �� MCHeli�� �w���R�v�^�[�܂��͌Œ藃�@ �ɗ^����_���[�W�̔{���B
DamageVsMCHeliTank = 1.0
	MCHeli�̋@�� �� MCHeli�� ��� �ɗ^����_���[�W�̔{���B
DamageVsMCHeliVehicle = 1.0
	MCHeli�̋@�� �� MCHeli�� �n�㕺�� �ɗ^����_���[�W�̔{���B
DamageVsMCHeliOther = 1.0
	MCHeli�̋@�� �� MCHeli�� ��L�ȊO�̃G���e�B�e�B �ɗ^����_���[�W�̔{���B

DamageMCHeliAircraftByExternal = 1.0
	���u�⑼��MOD�A�v���C���[�Ȃ� �� MCHeli�̃w���R�v�^�[�܂��͌Œ藃�@ �ɗ^����_���[�W�̔{���B
DamageMCHeliTankByExternal = 1.0
	���u�⑼��MOD�A�v���C���[�Ȃ� �� MCHeli�̐�� �ɗ^����_���[�W�̔{���B
DamageMCHeliVehicleByExternal = 1.0
	���u�⑼��MOD�A�v���C���[�Ȃ� �� MCHeli�̒n�㕺�� �ɗ^����_���[�W�̔{���B
DamageMCHeliOtherByExternal = 1.0
	���u�⑼��MOD�A�v���C���[�Ȃ� �� MCHeli�̑��̃G���e�B�e�B �ɗ^����_���[�W�̔{���B
	
DamageVsEntity = 3.0, flansmod
DamageMCHeliAircraftByExternal = 0.5, flansmod
DamageMCHeliVehicleByExternal = 0.5, flansmod
	DamageVsEntity �ɃG���e�B�e�B�̃N���X��������ƁA����̃G���e�B�e�B�̃_���[�W�{����ݒ�ł���B

IgnoreBulletHit = flansmod.common.guns.EntityBullet
	�w�肵�����O�̃G���e�B�e�B�ɒe��������Ȃ��悤�ɂ���B

--------------------------------------------------------------------------------------------------------------------

;CommandPermission = commandName(eg, modlist, status, fill...):playerName1, playerName2, playerName3...
;CommandPermission = modlist :example1, example2
;CommandPermission = status :  example2
OP�������Ȃ��v���C���[�ɂ� /mcheli �̃R�}���h���g�p�ł���悤�ɂ���B
CommandPermission = ������R�}���h�� :  �v���C���[��1, �v���C���[��2 ...
����1�F���̐ݒ�ŋ����Ă� PermissionsEx �Ȃǂ̃v���O�C���Ŗ���������邱�Ƃ�����B
����2�F�R�}���h�̂��� fill ��1.8��fill�R�}���h�̋����łŁA�u���b�N�u�����������300���ł���B
�@�@�@�����ɋ����o���ƃ}�b�v��^������ɂ���錜�O������B���͂悭�������邱�ƁB
����3�F�R�}���h�̂��� killentity,removeentity,attackentity �̓v���C���[�ɂ͌��ʂ��Ȃ����̂́A
�@�@�@���[�h����Ă���S�G���e�B�e�B���E�����Ƃ��ł���B���͂悭�������邱�ƁB

--------------------------------------------------------------------------------------------------------------------

[Key config]
http://minecraft.gamepedia.com/Key_codes

KeyUp = 17
KeyDown = 31
KeyRight = 32
KeyLeft = 30
KeySwitchGunner = 35
KeySwitchHovering = 57
KeySwitchWeapon1 = -98
KeySwitchWeapon2 = 34
KeySwitchWeaponMode = 45
KeyZoom = 44
KeyCameraMode = 46
KeyUnmountMob = 21
KeyFlare = 47
KeyExtra = 33
KeyCameraDistanceUp = 201
KeyCameraDistanceDown = 209
KeyFreeLook = 29
KeyGUI = 19
KeyGearUpDown = 48
KeyPutToRack = 36
KeyDownFromRack = 22
KeyScoreboard = 38
KeyMultiplayManager = 50
	�L�[�ݒ�B�@�̂�R�L�[�̕⋋��ʂ�MOD Option����ύX�\�Ȃ̂ŁA
	�����Őݒ肷�邱�Ƃ𐄏�����B
	��MOD option > Key Binding
