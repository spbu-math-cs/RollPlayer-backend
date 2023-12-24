# Creating maps for RollPlayer using Tiled

## Overview

You can create custom maps for RollPlayer using the popular map editor [Tiled](https://www.mapeditor.org/). RollPlayer supports Tiled Map JSON (`.tmj`) file format.

## Basic guide to Tiled

After downloading Tiled, open it and create new map. Set the tile width and height to 16px. Then add the tileset you'll be using for your map: `menu → Map → Add External Tileset...` We recommend using `tileset_packed_plus.tsj` provided with our app. We also recommend that, at the time of your editing, your map, the tileset being used by your map and the texture being used by the tileset will be in one folder, for the links in JSON files to be properly set up.

Then you can draw the map using the tiles of the tileset. First, select a tile in the *Tilesets* box (it's usually on the right; if it is not present, you can enable it (and any other window) via `menu → View → Views and Toolbars`). On a panel at the top, you will find some tools to help you with that:
- The *Stamp Brush* allows you to fill one cell with the selected tile or draw a line of cells. This is like the Pencil tool in MS Paint, which draws an 1-pixel wide line.
- The *Bucket Fill Tool* allows you to fill a continuous area of identical tiles with the selected tile. This corresponds to the Fill tool in Paint.
- The *Shape Fill Tool* allows you to paint a rectangle or a circle consisting of the selected tiles.
- The *Eraser* allows you to remove some tiles from a layer. This is good, for example, if you accidentally painted a tile on an obstacle layer on a wrong spot. This corresponds to the Paint tool of the same name.

You can select multiple tiles inside the *Tilesets* box to paint with a large brush consisting of all these tiles or enable *Random Mode* so that your brush will paint each cell with a randomly-chosen one of these tiles.

You can also select tiles on the map using tools like *Rectangular Select*, *Magic Wand* or *Select Same Tile*. When some tiles are selected, you will only be able to paint inside the selection. This is to help you avoid painting over a wrong tile by accident. To remove the selection, change to *Rectangular Select* and just click on the map anywhere.

There are also some useful tools like the complex *Terrain Brush*, but we encourage you to read about them in the official Tiled documentation (see link below.)

Note that our app does not support tile transparency, changing color of a layer or other similar special effects on tiles. Also, we do not support isometric or hexagonal orientation.

## Configuring the game behaviour of the tiles

The beautiful appearance of the map is important, but it's just not enough for a game. You need to configure which tile is obstacle, which tile it's hard to pass through, and which tile should hurt a character or give him some health.

To alter the behaviour of different tiles, you must divide your tiles into layers. All tiles of one layer will all be obstacles, other layer will give you +10 HP, etc. Keep that in mind. Before creating a map, we recommend creating all the layers you'll be using in the 'Layers' box.

Then you have to configure behaviour of different layers. This is what Custom Properties are for. In the Properties window (usually on the left, if it isn't there, you can always open it via `View`), you can change the properties of a map or a layer. To add a Custom Property, click on the header of the Custom Properties section with your right mouse button and choose 'Add Property'. You will also need to choose property type from a combo box. Do it in the following way:
- If a layer should be an obstacle layer (e.g. large stones, castle walls, sea, mountains), add a property named 'Obstacle', which should have type `bool` (you can think of `bool` as a checkbox) and check it.
- If a layer should impede movement (e. g. forest, bushes, gravel, sand, river), add a property named 'Pass cost', which has type `int` and set it to 2, 3, or 4 (the greater, the more difficult it will be to pass through). Pass cost 1 is the default pass cost of all tiles. Note that you can set Pass cost to 5 or more, but we don't recommend doing that, as the tiles of the layer may appear totally impassable.
- If a layer is a layer of bonus tiles that should heal your character or give it some mana, add a property named 'Restore HP' or 'Restore MP', which has type `int`, and set how many units of health or mana it should give to the character. Note that these tiles can be used only once. Also note that if a bonus tile has been already used, it won't be visible. It can be a good idea to place a bonus tile not distinguished from other tiles so that a character will suddenly get some HP/MP on these. 
- If a layer is harmful and dangerous, add a property named 'Lose HP' and 'Lose MP', which has type `int`, and set the number of HP/MP units the character should lose when stepping on a tile. Note that the trap tiles can only hurt a character once, like the bonus tiles.

If there are tiles from more than one layer on the same cell, then only the tile on top layer will be rendered, but this tile will inherit all properties from all these layers. If some of these layers have the same properties with different values, then the value of the topmost tile will be used.

You can click on an eye near the name of a layer in the 'Layers' box to hide all its tiles. This will help you see which tiles are on which layer to not accidentally paint blank tiles on an obstacle layer. Remember that the tiles themselves do not have any behaviour, so if even the tile looks like a wall, it's not an obstacle unless it's not on an obstacle layer!

## Saving your map

To use your map in RollPlayer, you need it to be saved in the JSON format. Choose 'JSON map files (*.tmj, *.json)' in the save box. Then, for the map to appear in the game, you need move it to `resources/maps/` subfolder of the app server's folder and launch the server locally. If you were using a custom tileset/texture for your map, you should also move them to `resources/tilesets/` and `resources/textures/`, respectively. You can also contact us at Telegram: ([@IDKWNTC](https://t.me/IDKWNTC), [@graphtreeheap](https://t.me/graphtreeheap)),  and request adding your map to the global server.

## More at...

For more information, read the official Tiled documentation by the link: https://doc.mapeditor.org/en/stable/
