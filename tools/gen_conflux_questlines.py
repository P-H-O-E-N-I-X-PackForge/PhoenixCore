"""Generates the Conflux per-discipline Chronicles questlines.

Writes SNBT quest files (the format Phoenix Chronicles loads from config/phoenix_chronicles/quests/) to
questlines/<chapter>/<id>.snbt, with a <chapter>.json that names the chapter and gives it an icon, and an index.txt
the mod uses to copy the chapters into config/phoenix_chronicles/quests/ the first time (a chapter that is already
there is left alone). Re-run this script after editing the tables below.

Every quest is hidden until the team has the discipline's first research flag (enable_if conflux:flag:...), so each
player only ever sees the questline of the discipline they chose.
"""
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'questlines')
CELL = 110


# ----------------------------------------------------------------- SNBT writer
def snbt(v):
    if isinstance(v, bool):
        return '1b' if v else '0b'
    if isinstance(v, int):
        return str(v)
    if isinstance(v, str):
        return '"' + v.replace('\\', '\\\\').replace('"', '\\"') + '"'
    if isinstance(v, list):
        return '[' + ','.join(snbt(x) for x in v) + ']'
    if isinstance(v, dict):
        return '{' + ','.join('%s:%s' % (k, snbt(x)) for k, x in v.items()) + '}'
    raise TypeError(type(v))


# ----------------------------------------------------------------- tasks / rewards
def research(flag, label):
    return {'type': 'conflux_research', 'flag_mode': True, 'flag': flag, 'label': label}


def dimension(dim):
    return {'type': 'dimension', 'target': dim}


def structure(sid):
    return {'type': 'structure', 'structure_id': sid}


def archive(entry):
    # PLACEHOLDER ids: Phoenix Archives entries still need to be written to match.
    return {'type': 'archive_entry', 'archive_entry_id': entry, 'optional': True}


def info(body):
    return {'type': 'info', 'body': body}


def have(item, count=1):
    return {'type': 'item_check', 'item_id': item, 'count': count, 'consume': False, 'sticky': True}


def xp(levels):
    return {'type': 'xp', 'levels': levels}


def item(item_id, count=1):
    return {'type': 'item', 'item_id': item_id, 'count': count, 'push_to_ae2': True}


def flag(f):
    # quests can lift a Conflux restriction without spending research: the reverse of the research flow
    return {'type': 'conflux_unlock', 'flag_mode': True, 'flag': f, 'node_id': 'phoenixcore:unknown'}


# ----------------------------------------------------------------- chapters
# folder -> (display name, icon, order)
CHAPTERS = {
    'conflux': ('The Conflux', 'minecraft:amethyst_cluster', 0),
    'phoenix': ('Phoenix', 'minecraft:beacon', 1),
    'sculk': ('Sculk', 'minecraft:sculk', 2),
    'void': ('Void', 'minecraft:spyglass', 3),
}


def discipline_chosen(disc):
    # state task from PhoenixCore: done while the team's discipline is this one (blank: any)
    return {'type': 'conflux_discipline', 'discipline': disc, 'label': 'Choose a discipline' if not disc else disc}


def trigger(trigger_id, count, label):
    # fired by PhoenixCore (see ChroniclesLink / PhoenixChroniclesTriggers) and counted by the quest
    return {'type': 'external_trigger', 'trigger_id': trigger_id, 'required': count, 'label': label}


# The hub chapter every player sees: the way in, and the choice that opens one of the discipline chapters below.
# (key, title, description, icon, (gx, gy), prereq keys, tasks, rewards, extras)
HUB = dict(
    chapter='CONFLUX', prefix='cf', gate=None,
    quests=[
        ('begin', 'The Conflux',
         'Something old and patient has been watching the work you do. It wants you to choose what you become.',
         'minecraft:amethyst_cluster', (0, 0), [], [info('Open the Conflux research screen and read what it offers.')],
         [xp(2)], {'subtitle': 'The Conflux'}),
        ('choose', 'Choose a Discipline',
         'Each discipline is a way of working, and the Conflux will only teach you one. Choose with care.',
         'minecraft:compass', (1, 0), ['begin'], [discipline_chosen('')], [xp(5)], {'subtitle': 'The Conflux'}),
    ])


# ----------------------------------------------------------------- questline tables
# (key, title, description, icon, (gx, gy), prereq keys, tasks, rewards, extras)
LINES = {}

# Only each discipline's own research quests live here. The shared template quests (arrival, realm, hall, shrine,
# terms, lore, commitment) were removed: they were identical in every tree.
LINES['phoenix'] = dict(
    chapter='PHOENIX', gate='px_awakened', dim='phoenixcore:conflux/phoenix', prefix='px',
    quests=[
        ('combustion', 'Controlled Burn', 'Fire is useful exactly as long as someone is in charge of it.',
         'minecraft:furnace', (0, 0), [], [research('px_combustion', 'Learn combustion')],
         [xp(6), item('minecraft:blaze_powder', 16)], {}),
        ('molten', 'The Molten Path', 'Metal remembers being liquid. Remind it.', 'minecraft:lava_bucket',
         (0, 1), ['combustion'], [research('px_molten_path', 'Walk the molten path')], [xp(8)], {}),
        ('apex', 'What Burns Last', 'Choose the way you will not go out.', 'minecraft:totem_of_undying',
         (0, 2), ['molten'], [research('px_undying_flame', 'Undying flame')], [xp(30)], {}),
        ('codex', 'The Ashen Codex', 'A book that predates the idea of a library. Start reading.',
         'minecraft:enchanted_book', (1, 2), ['molten'], [research('px_cx_root', 'Open the Codex')],
         [xp(10), item('minecraft:enchanted_book')], {}),
        ('dawn', 'The Dawn That Was Owed', 'The last page. It is blank, and warm, and yours.',
         'minecraft:beacon', (1, 3), ['codex'], [research('px_cx_dawn', 'Complete the Ashen Codex')],
         [xp(50), item('minecraft:nether_star', 2)], {'node_size': 'LARGE'}),
    ])

LINES['sculk'] = dict(
    chapter='SCULK', gate='sk_awakened', dim='phoenixcore:conflux/sculk', prefix='sk',
    quests=[
        ('mycelial', 'Mycelial Reach', 'Roots do not seek water. They simply grow.', 'minecraft:brown_mushroom',
         (0, 0), [], [research('sk_mycelial', 'Extend the mycelium')], [xp(6)], {}),
        ('echo', 'Echo Memory', 'The Sculk records the shape events leave behind.', 'minecraft:sculk_catalyst',
         (0, 1), ['mycelial'], [research('sk_echo_memory', 'Learn echo memory')],
         [xp(8), item('minecraft:echo_shard', 2)], {}),
        ('apex', 'A Mind of Many Places', 'Pick the way you will be remembered.', 'minecraft:recovery_compass',
         (0, 2), ['echo'], [research('sk_distributed_mind', 'Distributed mind')], [xp(30)], {}),
        ('library', 'The Listening Library', 'Sit. The chair faces nothing. The nothing sits with you.',
         'minecraft:enchanted_book', (1, 2), ['echo'], [research('sk_cx_root', 'Open the Reading Room')],
         [xp(10), item('minecraft:book', 8)], {}),
        ('epilogue', 'Epilogue, Unwritten', 'The Library finishes where you do. Take your time.',
         'minecraft:sculk_catalyst', (1, 3), ['library'], [research('sk_cx_epilogue', 'Finish the Library')],
         [xp(50), item('minecraft:nether_star', 2)], {'node_size': 'LARGE'}),
    ])

LINES['void'] = dict(
    chapter='VOID', gate='vd_awakened', dim='phoenixcore:conflux/void', prefix='vd',
    quests=[
        ('horizon', 'The Event Horizon', 'The point past which "later" stops being a place.',
         'minecraft:obsidian', (0, 0), [], [research('vd_event_horizon', 'Study the event horizon')],
         [xp(6)], {}),
        ('topology', 'Null Topology', 'A surface that is the same from every side, including inside.',
         'minecraft:chorus_flower', (0, 1), ['horizon'], [research('vd_null_topology', 'Map null topology')],
         [xp(8), item('minecraft:chorus_fruit', 8)], {}),
        ('apex', 'The Shape of Return', 'Choose whether you are a loop or a point.', 'minecraft:ender_chest',
         (0, 2), ['topology'], [research('vd_mobius', 'Mobius')], [xp(30)], {}),
        ('atlas', 'The Unlit Atlas', 'Maps of places that were never lit, correct anyway.', 'minecraft:map',
         (1, 2), ['topology'], [research('vd_cx_root', 'Open the Atlas')], [xp(10), item('minecraft:map', 4)], {}),
        ('final', 'Final Sheet, Unlit', 'Fold it, and everywhere you have been is one place.',
         'minecraft:end_portal_frame', (1, 3), ['atlas'], [research('vd_cx_final', 'Complete the Atlas')],
         [xp(50), item('minecraft:nether_star', 2)], {'node_size': 'LARGE'}),
    ])

# Sealed A / Sealed B had only the shared template quests, so they have no questline for now.


# ----------------------------------------------------------------- emit
def build(disc, line):
    prefix = line['prefix']
    gate = ('conflux:flag:' + line['gate']) if line.get('gate') else None
    count = 0
    outdir = os.path.join(OUT, disc)
    os.makedirs(outdir, exist_ok=True)
    write_chapter(disc)
    for key, title, desc, icon, (gx, gy), prereqs, tasks, rewards, extra in line['quests']:
        qid = '%s_q_%s' % (prefix, key)
        task_list = []
        for i, t in enumerate(tasks):
            t = dict(t)
            t['task_id'] = 'phoenixcore:conflux/%s/%s/t%d' % (disc, key, i)
            t['description'] = snbt_text(t.pop('label', t['type']))
            task_list.append(t)
        quest = {
            'id': qid,
            'title': title,
            'description': desc,
            'subtitle': 'Conflux - ' + disc.replace('_', ' ').title(),
            'chapter': line['chapter'],
            'icon_item': icon,
            'positionX': 160 + gx * CELL,
            'positionY': 80 + gy * CELL,
            'position_is_center': True,
            'shape': 'CIRCLE',
            'parent': 'none',
            'require_all_prereqs': True,
            'visibility': 'NORMAL',
            'prerequisites': [{'id': '%s_q_%s' % (prefix, p), 'required': True} for p in prereqs],
            'tasks': task_list,
            'rewards': rewards,
            'tags': ['conflux', disc],
        }
        if gate:
            quest['enable_if'] = gate
        quest.update(extra)
        with open(os.path.join(outdir, qid + '.snbt'), 'w', encoding='utf-8') as f:
            f.write(snbt(quest))
        count += 1
    return count


def write_chapter(folder):
    name, icon, order = CHAPTERS[folder]
    with open(os.path.join(OUT, folder, folder + '.json'), 'w', encoding='utf-8') as f:
        f.write('{"id":"%s","name":"%s","icon":"%s","order":%d,"background":null,"requirements":[]}'
                % (folder, name, icon, order))


# discipline -> (display name, colour). Each discipline is a category (a folder in the sidebar) holding its chapter, and
# it is shown only to a team that chose that discipline.
CATEGORIES = {
    'phoenix': ('Phoenix', 'FF8833'),
    'sculk': ('Sculk', '2FB5A8'),
    'void': ('Void', '8C6CFF'),
}


def write_categories():
    folder = os.path.join(OUT, 'categories')
    os.makedirs(folder, exist_ok=True)
    for disc, (name, color) in CATEGORIES.items():
        with open(os.path.join(folder, disc + '.yml'), 'w', encoding='utf-8', newline='\n') as f:
            f.write('id: %s\ndisplay_name: "%s"\ncolor: %s\nicon: "%s"\nenable_if: "conflux:discipline:%s"\n'
                    'chapters:\n  - %s\n' % (disc, name, color, CHAPTERS[disc][1], disc, disc.upper()))


def write_index():
    write_categories()
    lines = []
    for folder in list(CHAPTERS) + ['categories']:
        for name in sorted(os.listdir(os.path.join(OUT, folder))):
            lines.append('%s/%s' % (folder, name))
    with open(os.path.join(OUT, 'index.txt'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')


def snbt_text(text):
    # Chronicles stores task descriptions as a JSON text component
    return '{"text":"%s"}' % text.replace('"', '\\"')


if __name__ == '__main__':
    total = build('conflux', HUB)
    for disc, line in LINES.items():
        total += build(disc, line)
    write_index()
    print('wrote %d quests to %s' % (total, os.path.abspath(OUT)))
