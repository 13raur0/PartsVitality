# PartsVitality

**A hardcore damage and healing system for Minecraft, inspired by "Escape from Tarkov."**

PartsVitality completely overhauls Minecraft's standard health system by introducing a detailed part-based HP system. This plugin demands more tactical thinking, careful injury management, and a new level of awareness from players. Deciding which body part to target and which to protect, constantly monitoring your own condition and surroundings, and engaging in tense gameplay where a single moment of carelessness can be fatal becomes crucial.

## Key Features

- **Part-Based HP System**: Health is not a single bar but is managed individually across four body parts: "Head," "Chest," "Legs," and "Feet."
- **Precise Hit Detection**: Using Ray Tracing technology, the plugin accurately determines which body part an attack hits. Tactics like headshots or aiming for the legs become vital!
- **Realistic Injury Penalties**:
  - **Debuff Effects**: Taking damage to a specific part triggers corresponding debuffs (e.g., Nausea from head damage, Slowness from leg damage).
  - **Broken Parts**: When a part's HP drops to 0, it becomes "broken," reducing the player's maximum HP until they respawn.
- **Manual Healing System**:
  - **No Natural Regeneration**: Automatic health regeneration from saturation is disabled. All healing must be done manually.
  - **Part-Specific Healing**: Use items defined in `config.yml` to heal by clicking the corresponding armor piece for the injured part in the inventory.
  - **Surgery for Critical Injuries**: "Broken" parts cannot be healed with regular items. A special "surgery" item is required for first aid, making the part treatable again.
- **Enhanced Importance of Armor**:
  - **Part Protection**: Armor plays a crucial role in protecting the specific body part it covers.
  - **Realistic Wear and Tear**: Armor durability can be configured to decrease significantly based on the damage received, making it a critical resource to manage.
- **Highly Customizable**: Nearly every aspect of the plugin—part HP, damage multipliers, healing items, debuffs, and more—can be freely adjusted in `config.yml`.
- **Multi-Language Support**: All messages displayed to players are translatable. It supports English (`en`) and Japanese (`ja`) by default.

## Installation

1.  Download the latest `.jar` file.
2.  Place the downloaded `PartsVitality-1.0.0.jar` file into your server's `plugins/` folder.
3.  Restart or reload the server. The configuration file (`config.yml`) and language files (`messages_en.yml`, `messages_ja.yml`) will be automatically generated in the `plugins/PartsVitality/` folder.

## Configuration (`config.yml`)

You can edit the `config.yml` file to fine-tune the plugin's behavior to match your server's difficulty and playstyle.

<details>
<summary><strong>Click to see a detailed explanation of config.yml</strong></summary>

```yaml
# Set the language to use (e.g., en, ja)
language: "en"

# Max HP for each body part
parts:
  head:
    max-hp: 20.0
  chest:
    max-hp: 29.0
  # ... and so on

# Damage calculation settings
damage:
  # Multiplier to convert vanilla heart damage to part damage.
  # A higher value makes parts break more easily, increasing difficulty.
  damage-multiplier: 5.0
  # Precision of the hit detection. Larger values reduce server load but decrease accuracy. (Recommended: 0.1 ~ 0.5)
  ray-trace-step: 0.1

# Settings for regular healing
healing:
  # Time required for healing (in seconds).
  duration-seconds: 3
  # ... sound settings ...

# Settings for surgery (healing broken parts)
surgery:
  # Time required for surgery (in seconds).
  duration-seconds: 10
  # Amount of part HP restored by surgery. Setting this to 1.0 will make the part usable again and allow normal healing.
  restored-hp: 1.0
  # ... sound settings ...

# Max health penalty for each broken part (2.0 = 1 heart).
health-penalty-per-broken-part: 5.0 # 2.5 hearts

# Items used for regular healing and their heal amount.
healing-items:
  IRON_INGOT: 10.0
  GOLD_INGOT: 15.0

# Items used for surgery (only usable on parts with 0 HP).
surgery-items:
  DIAMOND: true

# Debuffs applied when a part's HP falls below a certain threshold.
debuffs:
  head:
    - threshold: 0.5 # 50% or less
      effect: CONFUSION
      level: 0
  # ... and so on

# Durability settings
durability:
  # If true, armor durability decreases based on the amount of part damage received.
  use-custom-durability-damage: true
  # How much part damage equals 1 point of durability damage.
  # A smaller value means armor wears out faster. 0.4 is balanced around leather armor.
  damage-per-durability-point: 0.4
```

</details>

## How to Play

- **Check Part HP**: In your inventory, hover over any armor piece and **Shift + Right-Click** to switch to Part HP display mode. The armor's durability bar will now show the HP of that part. Repeat the action to switch back.

  !Toggling Part HP Display

- **Heal a Part**: Hold a healing item (e.g., Iron Ingot) on your cursor and **Left-Click** the armor piece of the part you want to heal in your inventory. A timer will start, and upon completion, the part's HP will be restored.

  !Normal Healing Process

- **Perform Surgery**: If a part is broken (HP is 0), you must first treat it with a surgery item (e.g., Diamond). This will slightly restore the part's HP, allowing it to be healed with regular healing items again.

  !Surgery Process

## License

This plugin is released under the MIT License.
