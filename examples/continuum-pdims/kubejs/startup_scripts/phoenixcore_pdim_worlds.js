// PhoenixCore personal dimension worlds: a worked example of the whole Continuum pdim worldgen API.
//
// Copy this file to kubejs/startup_scripts/ in your pack, and the item tag from this folder's kubejs/data/ to your
// kubejs/data/. Without the Infiniverse mod a change needs a restart; with it, dimensions are made while the game runs.
// Every body used here is one of PhoenixCore's own (data/phoenix_continuum/continuum/bodies/*.json).
//
//   Anvil     home system, rocky and industrial      a shared colony: no plots, GregTech ores, a base waiting
//   Halo      home system, airless moon              vacuum: no air without a helmet, no mobs, protected plots
//   Rime      home system, ice world                 its own blizzard weather cycle
//   Meridian  home system, temperate ocean world     .auto(): worked out from the body's own data
//   Ember     Aurelian, volcanic crust               a nether-style world with the nether's ores
//   Cinder    Reliquary, scorched                    an ash storm over basalt (and a base at each plot)
//   Calliope  Carousel, pink gas giant               no ground: a platform in the storm, no air
//   Marrow    Thalassa, dark ocean world             a Conflux discipline world, with its structures and ores
//   Lantern   Vesper, glowing jungle                 raw biome and permanent rain
//
// Everything below the named worlds is a rule: it fills in every body that has no spec of its own.

ContinuumEvents.pdimWorldgen(event => {

  // ---------------------------------------------------------------------------------------------------------------
  // Anvil: an ordinary shared dimension, not a plot each.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:anvil')
    .noise('minecraft:overworld', 'minecraft:badlands')       // vanilla terrain, caves and structures, one biome
    .plots(false)                                             // everybody arrives in the same place
    .spawnAt(0, 70, 0)                                        // the x and z they arrive at (on the ground there)
    .gtOres('minecraft:overworld')                            // GregTech's overworld ore veins and bedrock fluids
    .weather('clear')                                         // never rains
    .plotStructure('minecraft:village/desert/houses/desert_small_house_1')  // put down once, for the whole dimension

  // ---------------------------------------------------------------------------------------------------------------
  // Halo: a vacuum moon. Every player has a plot, and nobody else can build on it.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:halo')
    .flat('minecraft:stony_peaks',                            // layers from the bottom up: "block" or "block*height"
      'minecraft:bedrock',
      'minecraft:deepslate*30',
      'minecraft:stone*28',
      'minecraft:tuff*2',
      'minecraft:gravel')
    .requireOxygen('#phoenixcore_examples:breathing_gear')    // no air: damage each second without one of these helmets
    .mobs(false)                                              // nothing spawns on its own
    .weather('clear')
    .plotRadius(300)                                          // players stay within 300 blocks of their own plot
    .protectPlots(true)                                       // nobody builds on a plot that is not theirs (or their team's)
    .gtOres('minecraft:overworld')

  // ---------------------------------------------------------------------------------------------------------------
  // Rime: weather that comes and goes by itself. Rain falls as snow in the cold biome.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:rime')
    .noise('minecraft:overworld', 'minecraft:ice_spikes')
    // clear for 5 to 20 minutes, a storm for 2 to 5, half of them with thunder
    .weatherCycle(300, 1200, 120, 300, 0.5)

  // ---------------------------------------------------------------------------------------------------------------
  // Meridian: a temperate ocean world. Nothing to say: .auto() reads the body (its ocean, its ice, its distance from the
  // star) and builds terrain to match: an ocean world near the right distance gets the overworld's own biomes.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:meridian').auto()

  // ---------------------------------------------------------------------------------------------------------------
  // Ember: the nether's terrain with a biome source of your own (an object), the nether's ores, and falls that hurt.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:ember')
    .noise('minecraft:nether', { type: 'minecraft:multi_noise', preset: 'minecraft:nether' })
    .gtOres('minecraft:the_nether')
    .fallDamage(true)
    .weather('clear')

  // ---------------------------------------------------------------------------------------------------------------
  // Cinder: ash over basalt. A permanent storm, nothing alive, and a base at every player's plot.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:cinder')
    .flat('minecraft:basalt_deltas',
      'minecraft:bedrock',
      'minecraft:basalt*40',
      'minecraft:blackstone*10',
      'minecraft:magma_block')
    .weather('storm')                                         // always thundering
    .mobs(false)
    .plotStructure('minecraft:village/desert/houses/desert_small_house_2')  // every player's plot gets one

  // ---------------------------------------------------------------------------------------------------------------
  // Calliope: a gas giant has no ground. Void (the default) with the obsidian pad, a storm and no air.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:calliope')
    .voidWorld()
    .weather('storm')
    .requireOxygen('#phoenixcore_examples:breathing_gear')

  // ---------------------------------------------------------------------------------------------------------------
  // Marrow: any chunk generator, even another mod's, with its own dimension type. This is a Conflux discipline world:
  // its terrain, structures and ores, on a Continuum planet. (A raw JSON object, so it needs PhoenixCore.)
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:marrow')
    .generator({
      type: 'phoenixcore:discipline',
      discipline_id: 'sculk',
      seed: 7312045,
      biome_source: { type: 'minecraft:fixed', biome: 'phoenixcore:sculk' }
    })
    .dimensionType('phoenixcore:discipline')
    .arriveOnSurface()

  // ---------------------------------------------------------------------------------------------------------------
  // Lantern: a jungle that never stops raining.
  // ---------------------------------------------------------------------------------------------------------------
  event.pdim('phoenix_continuum:lantern')
    .noise('minecraft:overworld', 'minecraft:jungle')
    .weather('rain')

  // ---------------------------------------------------------------------------------------------------------------
  // Rules. They fill in every body above that was not named. They are worked out when a dimension is made, so they need
  // the Infiniverse mod; a spec for a named body always wins over a rule.
  // ---------------------------------------------------------------------------------------------------------------

  // Every other moon is bare grey rock with no air (the same as Halo, without the protection).
  event.type('moon', spec => spec
    .flat('minecraft:stony_peaks', 'minecraft:bedrock', 'minecraft:stone*60', 'minecraft:gravel')
    .requireOxygen('#phoenixcore_examples:breathing_gear'))

  // The rest of the home system works itself out from its bodies.
  event.system('phoenix_continuum:home', spec => spec.auto())

  // Anything with a radius over 3 (a giant) is a platform in the sky.
  event.where(body => body.radius() > 3, spec => spec.voidWorld().weather('storm'))
})
