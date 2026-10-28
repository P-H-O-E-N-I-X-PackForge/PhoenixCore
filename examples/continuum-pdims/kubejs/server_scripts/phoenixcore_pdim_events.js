// Personal dimension events: a welcome and a starter kit the first time a player makes a world, and a log when they leave.
// Copy to kubejs/server_scripts/.

ContinuumEvents.pdimEnter(event => {
  if (!event.created) return   // only the first time this player made this world

  const player = event.player
  switch (event.body) {
    case 'phoenix_continuum:halo':
      // no air on Halo: a way to breathe, and a hint
      player.give('minecraft:turtle_helmet')
      player.tell('Halo has no air. Keep the helmet on; it is the only thing between you and the vacuum.')
      break
    case 'phoenix_continuum:calliope':
      player.give('minecraft:turtle_helmet')
      player.tell('You are on a platform in the storm of a gas giant. Do not go off the edge.')
      break
    case 'phoenix_continuum:anvil':
      player.tell('Anvil is shared: everybody who comes here arrives at the same place.')
      break
    case 'phoenix_continuum:cinder':
      player.give('minecraft:torch 16')
      player.tell('Cinder is under an ash storm. Your plot has a house; the rest is basalt.')
      break
    default:
      player.tell(`Welcome to ${event.body}.`)
  }
})

ContinuumEvents.pdimLeave(event => {
  console.info(`${event.player.name.string} left ${event.body}`)
})
