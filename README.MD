RemoteWorldLoader
=================

RemoteWorldLoader is a plugin which allows you to download world from OpenStack Swift containers, and upload them to these containers.

## How To Compile

Just run `mvn install` after compiling Spigot 1.8.3 using BuildTools.

## How To Use

Launch the plugin once, then edit `config.yml` file to use your OpenStack credentials.

The plugin comes with four commands :

### /load <container> [world]

This command requires permission `remoteworldloader.admin.load`. 
This command will load the container `container` as the world `world`. If no `world` parameter is provided, the `container` param is used as a target world name.

### /save <world> [container]

This command requires permission `remoteworldloader.admin.save`. 
This command will save the world `world` to the container `container`. If no `container` parameter is provided, the `world` param is used as a target container name.

### /reset <container>

This command requires permission `remoteworldloader.admin.reset`. 
This command will remote the container `container` on your swift server.

## /changeworld <world>

This command is available to all. The permission used is `remoteworldloader.user.changeworld`. 
The player running this command will be teleported to the world `world`.
Please note teleporting to a world requires the `remoteworldloader.accessworld.<world>` permission.
An user with `remoteworldloader.accessworld.*` permission allows you to join any world.