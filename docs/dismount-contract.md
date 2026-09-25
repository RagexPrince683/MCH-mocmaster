# Normal vehicle dismount contract

## Confirmed instant-dismount path

Minecraft polls the configured Sneak `KeyBinding` into `MovementInput.sneak`. The local player then
copies that state into its Sneak action packet, the server marks `EntityPlayerMP` as sneaking, and
vanilla `EntityPlayer.updateRidden` sees Sneak and calls `mountEntity(null)` before it delegates to
the superclass update. The former server guard targeted `EntityPlayerMP.onUpdate`, which is reached
only through that later superclass update and therefore ran after the detach branch. The normal
vanilla packet/detachment path could consequently bypass MC Heli's three-second request timer.

## Client input rule

Normal pilot and passenger exits use the physical state of Minecraft's configured Sneak binding;
both positive keyboard codes and negative mouse-button codes are supported. A hold belongs to one
player, world, connection, direct mount, parent vehicle, and seat. Release, death, a blocking GUI,
replay ownership, or any change to that context cancels it. Completion queues exactly one normal
dismount request and requires a physical release before another hold can begin.

The `MovementInputFromOptions` injection remains an early filter. A second guard runs at the head of
`EntityClientPlayerMP.onUpdateWalkingPlayer`, immediately before vanilla can publish movement state.
That latter boundary reads the current movement object, so replacement input implementations and
late handler writes cannot expose an incomplete hold. Neither guard changes Sneak on foot or while
riding a non-MC-Heli entity.

## Server authority rule

The client sends a mount-bound hold-start signal immediately and a cancellation on every reset. The
server records its own monotonic start time; no client elapsed duration is transmitted or trusted.
A normal exit request is accepted only after three server-observed seconds and only while the player
still rides the exact entity, parent, and seat from the start signal. Acceptance consumes the server
hold, so early, stale, duplicate, and wrong-mount requests are rejected.

As defense in depth, the `EntityPlayer.updateRidden` head clears early vanilla Sneak for a valid
direct MC Heli vehicle or seat rider, immediately before the actual `mountEntity(null)` branch. If
another client mod manages to publish Sneak, that first observation starts (but does not complete)
the server hold. Explicit ejection, parachuting, seat switching,
vehicle destruction, player death, invalid-seat cleanup, and rack operations continue through their
existing independent branches and are not treated as normal Sneak dismount requests.

## Diagnostics

SimpleImpl invokes packet handlers on its network event loop in this Forge generation. The wrapper
therefore schedules both client callbacks and server callbacks onto their respective game threads
before hold state or any vehicle/entity state is read or mutated.

With the existing MC Heli debug logging option enabled, bounded transition-only messages identify
client starts, resets, completion, request consumption, packet transmission, server start/cancel,
acceptance/rejection, mount identity, seat identity, elapsed server time, and rejection reason.
An attempted early vanilla detach also identifies the server side, exact vanilla caller, mount and
parent identities, seat, server hold state, and elapsed time.
