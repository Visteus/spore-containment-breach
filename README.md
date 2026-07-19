# Spore: Containment Breach
A Minecraft mod aiming to expand on (Fungal Infection: Spore)[https://www.curseforge.com/minecraft/mc-mods/fungal-infection-spore]'s core to create a different "feel".

Instead of spreading from any dark place, given a bit of time, or from a single location like (Spore Inquisition)[https://www.curseforge.com/minecraft/mc-mods/spore-inquisition] handles it, here the infection can spread from any "spreader" location, like the Hospitals, Cathedrals, Mass Graves, and other structures. What starts as a single flesh mound can spread over time, corrupting the world leading to make you choose: stand and fight back the infection, or move on to a new area?

*Built with help from a __local__ LLM on decompiling and creating hooks/mixins into the existing Spore mod. Dont give big tech your money if you can help it!*

## Change Summary
### New Spawning
Disabled "vanilla" spawn system, so that you won't run into Infected in every cave system. Instead, you have to look out for Spore structures (configurable to include other mods' structures) as they act as the source of the infection. 

Within each configured structure, at least one Flesh Mound will live, alongside its defenders. Over time it will grow a defensive structure around it, before placing a Reconstructed Mind which can grow into a Proto-Hivemind, as usual. If this happens, expect the Infection in the area to ramp up, as well as the difficulty in clearing it. 

### Growing Structures, Spreading Biomes
Similar to `Spore: Inquisition`, the infection (if left unchecked) has the potential to grow organically, creating effectively its own biome of meat and bile. Not just infecting blocks, but building heretical towers off the bodies of things unlucky enough to be nearby. This creates "fortress-like" structures where going in to clear out the infection isn't a simple feat, and some modpacks may resort to WMDs to clear out infections that crop up before they get out of hand. 

### Technical changes
Beyond the obvious changes, this mod also overwrites other behaviors like how certain organoids chunkload, how new Mounds come into being, etc. The goal is to keep the threat "alive", but hopefully less lag-creating than similar mods by liberally lobotomizing entities (noAI) that dont need to be active. 


## Configuration
Nearly every part of the mod has levers to adjust, as this is built first and foremost to be included in modpacks like my own (Fungal Engines)[https://www.curseforge.com/minecraft/modpacks/fungal-engines].

>Have a structure you want infected?
>>Add its tag(s) to the config!

>Want things to be on the lighter side for a kitchen-sink pack, as a "dash" of infection? 
>>Reduce spreading/chunkloading ranges and valid structures, etc.

And more!