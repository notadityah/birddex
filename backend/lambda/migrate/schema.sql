-- better-auth v1.5 tables — camelCase columns required by Kysely adapter
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

-- App tables — snake_case, queried directly with postgres.js
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

-- Seed bird data
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

-- Ensure bird id sequence is in sync with seed data
SELECT setval(pg_get_serial_sequence('bird', 'id'), GREATEST((SELECT MAX(id) FROM bird), 1));

-- Gallery columns
ALTER TABLE sighting ADD COLUMN IF NOT EXISTS public BOOLEAN DEFAULT FALSE;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS gallery_anonymous BOOLEAN DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_sighting_public ON sighting(public) WHERE public = TRUE;
CREATE INDEX IF NOT EXISTS idx_bird_slug ON bird(slug);

-- Seed initial admin
UPDATE "user" SET role = 'admin'
WHERE email = 'imadityahariharan@gmail.com' AND role IS DISTINCT FROM 'admin';
