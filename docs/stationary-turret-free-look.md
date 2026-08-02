# Stationary turret free look

Stationary turrets are custom `MCH_EntityTurret` vehicles: the pilot rides the entity directly, while extra occupants use the shared aircraft seat entities. Their client tick handler, renderer, camera dummy, and player-control packet are turret-specific, but mouse processing and free-look status come from the shared base-vehicle implementation.

Previously, the turret disabled `canSwitchFreeLook`, omitted the free-look key from its polled key list, and ignored the packet field on the server. Consequently the shared mouse path always coupled player yaw and pitch to entity yaw and pitch. Unrestricted weapons also temporarily used the rider's rotation as their firing rotation.

The turret now opts into the existing base-vehicle free-look status and handles that status through its existing control packet. While active, shared mouse processing changes the player's view but zeros turret rotation input, so the entity and synchronized rider-aim angles remain at their last aim. Weapon use temporarily applies those held aim angles instead of the free-look camera angles. On exit, the client camera recenters toward that held aim before asking the server to disable free look; this avoids snapping either the camera or turret. The temporary recenter state belongs only to the turret client handler and is cleared whenever the ridden turret changes or disappears.

The same camera object and dummy render-view entity are used in first- and third-person views, so both modes follow the separated camera rotation. No aircraft, tank, ship, UAV, drone, or ordinary gunner-seat controls are changed.
