-- ==========================================================================
-- better-auth tables
-- ==========================================================================
-- These tables use camelCase column names (e.g. "emailVerified", "createdAt")
-- because better-auth's Kysely adapter expects this exact casing. Do not
-- rename to snake_case — it will break auth.
-- ==========================================================================

CREATE TABLE IF NOT EXISTS "user" (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  "emailVerified" BOOLEAN NOT NULL DEFAULT FALSE,
  image TEXT,
  "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
  "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS session (
  id TEXT PRIMARY KEY,
  "expiresAt" TIMESTAMP NOT NULL,
  token TEXT NOT NULL UNIQUE,
  "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
  "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW(),
  "ipAddress" TEXT,
  "userAgent" TEXT,
  "userId" TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS account (
  id TEXT PRIMARY KEY,
  "accountId" TEXT NOT NULL,
  "providerId" TEXT NOT NULL,
  "userId" TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
  "accessToken" TEXT,
  "refreshToken" TEXT,
  "idToken" TEXT,
  "accessTokenExpiresAt" TIMESTAMP,
  "refreshTokenExpiresAt" TIMESTAMP,
  scope TEXT,
  password TEXT,
  "createdAt" TIMESTAMP NOT NULL DEFAULT NOW(),
  "updatedAt" TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS verification (
  id TEXT PRIMARY KEY,
  identifier TEXT NOT NULL,
  value TEXT NOT NULL,
  "expiresAt" TIMESTAMP NOT NULL,
  "createdAt" TIMESTAMP,
  "updatedAt" TIMESTAMP
);

-- Admin plugin columns (better-auth admin plugin)
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'user';
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS banned BOOLEAN DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "banReason" TEXT;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS "banExpires" TIMESTAMP;
ALTER TABLE session ADD COLUMN IF NOT EXISTS "impersonatedBy" TEXT;

-- ==========================================================================
-- App tables — snake_case, queried directly with postgres.js tagged templates
-- ==========================================================================
CREATE TABLE IF NOT EXISTS bird (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  scientific_name TEXT NOT NULL,
  slug TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS sighting (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
  bird_id INT NOT NULL REFERENCES bird(id),
  image_key TEXT,
  detected_at TIMESTAMP,
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS sighting_user_id_idx ON sighting(user_id);
CREATE INDEX IF NOT EXISTS sighting_bird_id_idx ON sighting(bird_id);

-- Seed data: 36 common Australian bird species.
-- These match the ONNX model's class labels (slug = model output label).
-- ON CONFLICT DO NOTHING so re-running the migration is safe.
INSERT INTO bird (id, name, scientific_name, slug) VALUES
  (1, 'Australian King Parrot', 'Alisterus scapularis', 'australian_king_parrot'),
  (2, 'Australian Magpie', 'Gymnorhina tibicen', 'australian_magpie'),
  (3, 'Australian Raven', 'Corvus coronoides', 'australian_raven'),
  (4, 'Black-faced Cuckoo-shrike', 'Coracina novaehollandiae', 'black_faced_cuckoo_shrike'),
  (5, 'Common Blackbird', 'Turdus merula', 'common_blackbird'),
  (6, 'Common Myna', 'Acridotheres tristis', 'common_myna'),
  (7, 'Crested Pigeon', 'Ocyphaps lophotes', 'crested_pigeon'),
  (8, 'Crimson Rosella', 'Platycercus elegans', 'crimson_rosella'),
  (9, 'Eastern Spinebill', 'Acanthorhynchus tenuirostris', 'eastern_spinebill'),
  (10, 'Eastern Yellow Robin', 'Eopsaltria australis', 'eastern_yellow_robin'),
  (11, 'Galah', 'Eolophus roseicapilla', 'galah'),
  (12, 'Grey Butcherbird', 'Cracticus torquatus', 'grey_butcherbird'),
  (13, 'Grey Fantail', 'Rhipidura albiscapa', 'grey_fantail'),
  (14, 'House Sparrow', 'Passer domesticus', 'house_sparrow'),
  (15, 'Koel', 'Eudynamys orientalis', 'koel'),
  (16, 'Laughing Kookaburra', 'Dacelo novaeguineae', 'laughing_kookaburra'),
  (17, 'Little Wattlebird', 'Anthochaera chrysoptera', 'little_wattlebird'),
  (18, 'Magpie-lark', 'Grallina cyanoleuca', 'magpie_lark'),
  (19, 'New Holland Honeyeater', 'Phylidonyris novaehollandiae', 'new_holland_honeyeater'),
  (20, 'Noisy Miner', 'Manorina melanocephala', 'noisy_miner'),
  (21, 'Pied Butcherbird', 'Cracticus nigrogularis', 'pied_butcherbird'),
  (22, 'Pied Currawong', 'Strepera graculina', 'pied_currawong'),
  (23, 'Purple Swamphen', 'Porphyrio porphyrio', 'purple_swamphen'),
  (24, 'Rainbow Lorikeet', 'Trichoglossus moluccanus', 'rainbow_lorikeet'),
  (25, 'Red-browed Finch', 'Neochmia temporalis', 'red_browed_finch'),
  (26, 'Red Wattlebird', 'Anthochaera carunculata', 'red_wattlebird'),
  (27, 'Red-whiskered Bulbul', 'Pycnonotus jocosus', 'red_whiskered_bulbul'),
  (28, 'Rock Dove', 'Columba livia', 'rock_dove'),
  (29, 'Silvereye', 'Zosterops lateralis', 'silvereye'),
  (30, 'Spotted Pardalote', 'Pardalotus punctatus', 'spotted_pardalote'),
  (31, 'Spotted Turtle-dove', 'Spilopelia chinensis', 'spotted_turtle_dove'),
  (32, 'Sulphur-crested Cockatoo', 'Cacatua galerita', 'sulphur_crested_cockatoo'),
  (33, 'Superb Fairy-wren', 'Malurus cyaneus', 'superb_fairy_wren'),
  (34, 'Welcome Swallow', 'Hirundo neoxena', 'welcome_swallow'),
  (35, 'Willie Wagtail', 'Rhipidura leucophrys', 'willie_wagtail'),
  (36, 'Yellow-tailed Black Cockatoo', 'Zanda funerea', 'yellow_tailed_black_cockatoo')
ON CONFLICT (id) DO NOTHING;

-- Reset the bird ID sequence to match seed data so new inserts get id > 36
SELECT setval(pg_get_serial_sequence('bird', 'id'), GREATEST((SELECT MAX(id) FROM bird), 1));

-- Gallery columns
ALTER TABLE sighting ADD COLUMN IF NOT EXISTS public BOOLEAN DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS gallery_anonymous BOOLEAN DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_sighting_public ON sighting(public) WHERE public = TRUE;
CREATE INDEX IF NOT EXISTS idx_bird_slug ON bird(slug);

-- Feedback table
CREATE TABLE IF NOT EXISTS feedback (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
  category TEXT NOT NULL,
  message TEXT NOT NULL,
  page_url TEXT,
  status TEXT NOT NULL DEFAULT 'open',
  admin_notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status);
-- Optional image attached to a feedback report (e.g. a "wrong match" detect report)
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS image_key TEXT;

-- Seed initial admin
UPDATE "user" SET role = 'admin'
WHERE email = 'imadityahariharan@gmail.com' AND role IS DISTINCT FROM 'admin';

-- ==========================================================================
-- Pokédex stats columns (detect-screen upgrade)
-- ==========================================================================
ALTER TABLE bird ADD COLUMN IF NOT EXISTS wingspan TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS length TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS weight TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS conservation_status TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS habitat TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS diet TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS call_description TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS fun_fact TEXT;
ALTER TABLE bird ADD COLUMN IF NOT EXISTS rarity TEXT DEFAULT 'common';
ALTER TABLE bird ADD COLUMN IF NOT EXISTS appearance TEXT;

-- Seed curated stats (values from Birds in Backyards / Australian Museum
-- references). Keyed by slug and re-runnable: an UPDATE against the same
-- values is a no-op, so the whole file stays idempotent.
UPDATE bird b SET
  wingspan = v.wingspan, length = v.length, weight = v.weight,
  conservation_status = v.cons, habitat = v.habitat, diet = v.diet,
  call_description = v.call, fun_fact = v.fact, rarity = v.rarity,
  appearance = v.appearance
FROM (VALUES
  ('australian_king_parrot', '55–60 cm', '41–43 cm', '195–275 g', 'Least Concern',
   'Wet forests, rainforest edges, well-treed gardens', 'Seeds, fruit, berries, nectar',
   'High-pitched piping whistle in flight; rolling "carr-ack"',
   'Males are the only Australian parrots with a completely red head.',
   'uncommon', 'Male has scarlet head and underparts with green wings; female has a green head and breast with a red belly.'),
  ('australian_magpie', '65–85 cm', '37–43 cm', '220–350 g', 'Least Concern',
   'Open woodland, parks, gardens, farmland', 'Insects, larvae, worms, small vertebrates',
   'Rich, melodious carolling, especially at dawn',
   'Can recognise and remember individual human faces for years.',
   'common', 'Bold black-and-white bird with a white nape and shoulders, red-brown eyes and a sturdy blue-white bill.'),
  ('australian_raven', '90–100 cm', '46–53 cm', '550–700 g', 'Least Concern',
   'Woodland, farmland, urban areas', 'Insects, carrion, grain, refuse, small animals',
   'Long, wailing "aah-aah-aaaahh" fading at the end',
   'Its drawn-out death-wail call distinguishes it from other corvids.',
   'common', 'Large all-black bird with a heavy bill, white eyes and prominent throat hackles.'),
  ('black_faced_cuckoo_shrike', '70–80 cm', '32–34 cm', '100–130 g', 'Least Concern',
   'Open forests, woodlands, parks and gardens', 'Insects, larvae, fruit',
   'Soft churring "creearck" given in flight',
   'Often shuffles and refolds its wings on landing, earning the nickname "shufflewing".',
   'uncommon', 'Sleek blue-grey bird with a jet-black face and throat and a gently undulating flight.'),
  ('common_blackbird', '34–38 cm', '25–26 cm', '80–100 g', 'Least Concern',
   'Urban gardens, parks, orchards (introduced)', 'Insects, earthworms, snails, fruit',
   'Rich, mellow fluting song, often at dusk',
   'Introduced from Europe in the 1850s; males sing from high perches at dawn and dusk.',
   'common', 'Male is all black with a bright orange-yellow bill and eye-ring; female is dark brown with a mottled breast.'),
  ('common_myna', '40–45 cm', '23–26 cm', '110–140 g', 'Least Concern',
   'Urban areas, farmland (introduced)', 'Insects, fruit, scraps, pet food',
   'Loud, scratchy chattering and gurgles',
   'Roosts communally in groups of up to several thousand birds.',
   'common', 'Chocolate-brown body with a glossy black head, yellow bill, legs and bare yellow skin behind the eye; white wing patches flash in flight.'),
  ('crested_pigeon', '45–50 cm', '30–34 cm', '150–250 g', 'Least Concern',
   'Open woodland, parks, sports fields, roadsides', 'Seeds, green shoots, small insects',
   'Distinctive whistling wingbeat on take-off; soft "whoop"',
   'The whistling sound in flight is made by air rushing over a modified wing feather.',
   'common', 'Grey pigeon with a thin upright black crest, pinkish flanks and iridescent bronze wing patches barred with black.'),
  ('crimson_rosella', '44–53 cm', '32–36 cm', '115–170 g', 'Least Concern',
   'Wet forests, mountain woodlands, leafy gardens', 'Seeds, fruit, blossoms, nectar, insects',
   'Bell-like "cussik-cussik" and soft piping whistles',
   'Young birds are largely green and gradually turn crimson over 15 months.',
   'common', 'Rich crimson body with bright blue cheeks, blue wings and tail, and black-scalloped back feathers.'),
  ('eastern_spinebill', '22–25 cm', '13–16 cm', '10–12 g', 'Least Concern',
   'Heathland, forests, gardens with tubular flowers', 'Nectar, small insects',
   'Rapid, high-pitched piping notes',
   'Can hover briefly while feeding on nectar, much like a hummingbird.',
   'rare', 'Tiny honeyeater with a long, fine down-curved bill, cinnamon underparts, white throat patch and black head band.'),
  ('eastern_yellow_robin', '25–27 cm', '15–16 cm', '18–22 g', 'Least Concern',
   'Wet forests, woodland understorey, shaded gullies', 'Insects, spiders, other arthropods',
   'Repeated piping "chop chop"; bell-like notes at dawn',
   'Often perches sideways on tree trunks, scanning the ground for prey.',
   'uncommon', 'Grey-backed robin with a bright yellow breast and belly and a pale grey throat; upright, confiding posture.'),
  ('galah', '70–80 cm', '34–38 cm', '270–350 g', 'Least Concern',
   'Open grasslands, farmland, urban parks', 'Seeds, grain, green shoots, roots',
   'High-pitched screeching "chi-chi" in flight',
   'Pairs bond for life, and flocks perform spectacular acrobatics in windy weather.',
   'common', 'Rose-pink face and underparts with soft grey back and wings and a pale pink crest.'),
  ('grey_butcherbird', '37–43 cm', '24–30 cm', '80–110 g', 'Least Concern',
   'Woodlands, urban parks and gardens', 'Insects, small birds, lizards, mice',
   'Beautiful rollicking, piping song, often in duet',
   'Wedges surplus prey into tree forks — a habit that earned the family its name.',
   'common', 'Grey back with black head, white collar and underparts, and a hooked, silver-grey bill with a black tip.'),
  ('grey_fantail', '18–22 cm', '14–17 cm', '7–9 g', 'Least Concern',
   'Forests, woodlands, gardens — almost anywhere with trees', 'Flying insects',
   'Thin, squeaky violin-like song',
   'Almost never sits still — it fans its tail and pirouettes constantly to flush insects.',
   'common', 'Small grey bird with a white eyebrow and throat, buff belly, and a long tail fanned wide and swung from side to side.'),
  ('house_sparrow', '21–25 cm', '14–16 cm', '24–32 g', 'Least Concern',
   'Urban areas, farms (introduced)', 'Seeds, grain, insects, scraps',
   'Persistent chirping "cheep cheep"',
   'One of the world''s most widespread birds, introduced to Australia in the 1860s.',
   'common', 'Male has a grey crown, black bib and chestnut nape; female is plain grey-brown with a pale eyebrow.'),
  ('koel', '95–105 cm', '39–46 cm', '190–240 g', 'Least Concern',
   'Tall trees in suburbs, rainforest edges (spring–summer migrant)', 'Fruit, especially figs',
   'Loud, rising "koo-ell" repeated endlessly, often at night',
   'A brood parasite — it lays its eggs in the nests of wattlebirds and friarbirds.',
   'uncommon', 'Male is glossy blue-black with a red eye; female is brown with white spots and heavy barring below.'),
  ('laughing_kookaburra', '60–66 cm', '40–47 cm', '300–500 g', 'Least Concern',
   'Open forests, woodlands, parks with old trees', 'Insects, lizards, snakes, rodents, small birds',
   'Famous raucous laughing chorus at dawn and dusk',
   'The world''s largest kingfisher, and its "laugh" is a territorial claim, not amusement.',
   'common', 'Stocky bird with a huge bill, whitish head and underparts, brown eye-stripe and back, and blue-flecked wings.'),
  ('little_wattlebird', '38–45 cm', '26–30 cm', '55–75 g', 'Least Concern',
   'Banksia heathland, coastal scrub, gardens', 'Nectar, especially banksia; insects',
   'Harsh cackling "cookay-cok" and squeaky chatters',
   'Despite the name, it lacks the neck wattles of other wattlebirds.',
   'common', 'Grey-brown honeyeater heavily streaked with white, with rufous wing patches visible in flight and a blue-grey eye.'),
  ('magpie_lark', '40–45 cm', '25–30 cm', '65–120 g', 'Least Concern',
   'Open areas near water, urban lawns, parks', 'Insects, worms, small invertebrates',
   'Metallic "pee-wee" duet sung in perfect alternation',
   'Pairs sing precisely-timed duets — each bird contributes alternate notes.',
   'common', 'Neat black-and-white bird smaller than a magpie; male has a black throat and white eyebrow, female a white face and throat.'),
  ('new_holland_honeyeater', '24–26 cm', '17–18 cm', '18–23 g', 'Least Concern',
   'Heathland, banksia scrub, flowering gardens', 'Nectar, insects, honeydew',
   'Sharp "tjik" and loud whistled chatter',
   'Among the first Australian birds scientifically described, back in 1790.',
   'common', 'Striking black-and-white streaked honeyeater with large yellow wing panels, yellow tail edges and a white eye.'),
  ('noisy_miner', '36–40 cm', '24–28 cm', '60–80 g', 'Least Concern',
   'Open woodland, urban parks and gardens', 'Nectar, insects, fruit',
   'Constant piping alarm calls and chattering',
   'Lives in aggressive colonies that mob intruders and drive smaller birds away.',
   'common', 'Grey honeyeater with a black crown and cheeks, yellow bill, yellow bare skin behind the eye and yellow legs.'),
  ('pied_butcherbird', '50–55 cm', '33–37 cm', '110–130 g', 'Least Concern',
   'Open woodland, farmland with scattered trees', 'Insects, small vertebrates, some fruit',
   'Superb flute-like caroling, often at night',
   'Widely considered one of the finest songbirds in the world.',
   'uncommon', 'Crisp black hood and bib over white collar and underparts, black back with white wing stripes and rump.'),
  ('pied_currawong', '70–80 cm', '44–50 cm', '280–320 g', 'Least Concern',
   'Forests and woodlands; urban areas in winter', 'Insects, fruit, small birds, carrion',
   'Loud ringing "curra-wong" and wolf whistles',
   'Named for its call; mountain populations move into lowland towns each winter.',
   'common', 'Large black bird with bright yellow eyes and bold white patches on wings, rump and tail tip.'),
  ('purple_swamphen', '80–90 cm', '44–48 cm', '800–1,050 g', 'Least Concern',
   'Wetlands, reed beds, lake edges, urban ponds', 'Reed shoots, aquatic plants, frogs, eggs',
   'Loud harsh screeches and penetrating "kee-ow"',
   'Holds food in one long-toed foot like a hand while eating.',
   'uncommon', 'Large deep-blue waterbird with a heavy red bill and frontal shield, red legs and a flicking white undertail.'),
  ('rainbow_lorikeet', '45–46 cm', '25–30 cm', '100–160 g', 'Least Concern',
   'Coastal forests, urban gardens with flowering trees', 'Nectar, pollen, fruit, blossoms',
   'Constant shrill screeching and chattering',
   'Its brush-tipped tongue is specialised for harvesting nectar and pollen.',
   'common', 'Unmistakable: deep blue head and belly, bright green wings, orange-red breast and a red bill.'),
  ('red_browed_finch', '15–17 cm', '11–12 cm', '9–11 g', 'Least Concern',
   'Grassy areas near dense cover, creek edges, gardens', 'Grass seeds, small insects',
   'High thin "seee" contact calls',
   'Travels in bouncing flocks that "explode" into cover when startled.',
   'common', 'Small olive-and-grey finch with a bright red eyebrow, rump and bill.'),
  ('red_wattlebird', '50–55 cm', '33–37 cm', '100–120 g', 'Least Concern',
   'Open forests, gardens with nectar plants', 'Nectar, insects, some fruit',
   'Harsh coughing bark and gurgling chatter',
   'One of Australia''s largest honeyeaters, named for its red neck wattles.',
   'common', 'Big streaked grey-brown honeyeater with red eye-wattles, a yellow belly and white facial streaks.'),
  ('red_whiskered_bulbul', '25–28 cm', '20–22 cm', '25–40 g', 'Least Concern',
   'Suburban gardens, parks (introduced)', 'Fruit, berries, insects, flower buds',
   'Cheerful whistled "kink-a-joo"',
   'Native to Asia; established around Sydney and Melbourne from cage-bird releases in the 1880s.',
   'uncommon', 'Jaunty brown bird with a tall black crest, red cheek-whisker patch, white underparts and a red vent.'),
  ('rock_dove', '60–68 cm', '30–35 cm', '240–380 g', 'Least Concern',
   'Cities, towns, bridges, grain stores (introduced)', 'Seeds, grain, scraps',
   'Soft rolling "coo-roo-coo"',
   'The familiar city pigeon — descended from cliff-nesting doves domesticated over 5,000 years ago.',
   'common', 'Typically blue-grey with two black wing bars and iridescent green-purple neck sheen, though plumage varies widely.'),
  ('silvereye', '15–18 cm', '11–13 cm', '9–11 g', 'Least Concern',
   'Almost any shrubby habitat: forests, heath, gardens, orchards', 'Insects, nectar, fruit',
   'Soft warbling song; thin "psee" contact calls',
   'Tasmanian silvereyes migrate over Bass Strait to the mainland each winter.',
   'common', 'Tiny olive-green bird with a conspicuous ring of white feathers around each eye and a grey back.'),
  ('spotted_pardalote', '13–15 cm', '8–10 cm', '8–10 g', 'Least Concern',
   'Eucalypt forest canopy; nests in earth-bank tunnels', 'Lerps, scale insects, other tiny invertebrates',
   'Soft, repeated three-note "sleep-may-be"',
   'One of Australia''s smallest birds — it digs a nesting tunnel up to 1 m into earthen banks.',
   'rare', 'Jewel-like: black wings, head and tail sprinkled with white spots, yellow throat and a red rump.'),
  ('spotted_turtle_dove', '43–48 cm', '28–32 cm', '150–170 g', 'Least Concern',
   'Urban gardens, parks, farmland (introduced)', 'Seeds, grain, scraps',
   'Gentle rolling "coo-croo-coo, coo"',
   'Introduced from Asia in the 1860s; the "spotted" collar is absent in young birds.',
   'common', 'Soft grey-brown dove with a black nape collar finely spotted with white.'),
  ('sulphur_crested_cockatoo', '100–110 cm', '44–51 cm', '700–950 g', 'Least Concern',
   'Forests, woodlands, urban parks', 'Seeds, roots, berries, nuts, bulbs',
   'Ear-splitting harsh screech, especially at roosts',
   'Can live for more than 80 years and posts "sentinel" birds while the flock feeds.',
   'common', 'Large white cockatoo with an expressive sulphur-yellow crest and a pale yellow wash under wings and tail.'),
  ('superb_fairy_wren', '13–15 cm', '13–14 cm', '8–13 g', 'Least Concern',
   'Dense shrubs bordering open grassy areas, gardens', 'Insects, other small arthropods',
   'Rapid, high-pitched reeling trill',
   'Breeding males present yellow flower petals to females during courtship.',
   'common', 'Breeding male has an iridescent sky-blue crown, cheeks and back with a black mask; females and non-breeders are plain brown with a reddish eye-ring.'),
  ('welcome_swallow', '25–30 cm', '14–15 cm', '12–16 g', 'Least Concern',
   'Open country near water; nests on buildings and bridges', 'Flying insects caught on the wing',
   'Twittering chatter given in flight',
   'Named because its arrival signalled the coming of spring to early settlers.',
   'common', 'Graceful swallow with glossy blue-black upperparts, rusty forehead and throat, and a deeply forked tail.'),
  ('willie_wagtail', '25–30 cm', '19–22 cm', '17–24 g', 'Least Concern',
   'Open areas, lawns, paddocks — nearly Australia-wide', 'Insects caught on the ground or in the air',
   'Sweet "sweet-pretty-creature" song, day and night',
   'Fearlessly harasses eagles and kookaburras many times its size.',
   'common', 'Black above and white below with white eyebrows it raises and lowers to signal mood; wags its fanned tail restlessly.'),
  ('yellow_tailed_black_cockatoo', '95–110 cm', '55–65 cm', '750–900 g', 'Least Concern',
   'Eucalypt forest, pine plantations, coastal heath', 'Wood-boring grubs, banksia and pine seeds',
   'Eerie, far-carrying wailing "kee-ow" in flight',
   'Tears open tree trunks with its massive bill to extract wood-boring grubs.',
   'rare', 'Huge dusky-black cockatoo with pale feather edging, yellow cheek patches and broad yellow tail panels.')
) AS v(slug, wingspan, length, weight, cons, habitat, diet, call, fact, rarity, appearance)
WHERE b.slug = v.slug;
