package com.birddex.app.data.birds

/** Who took a reference photo, under which licence, and where it came from. */
data class PhotoCredit(val author: String, val license: String, val sourceUrl: String)

/**
 * The reference photo of each species, served by the website at `<site>/birds/<slug>.jpg`
 * (`frontend/public/birds/`). They come from Wikimedia Commons, nearly all under CC BY /
 * CC BY-SA, which require crediting the photographer with a link to the source wherever a
 * photo is shown (one is CC0, credited the same way).
 *
 * Copied from `frontend/src/data/birdImages.js`; keep the two in step when photos change.
 */
object BirdImages {

    /** `https://birddex.fun` + `/birds/house_sparrow.jpg`. */
    fun url(baseUrl: String, slug: String): String = "${baseUrl.trimEnd('/')}/birds/$slug.jpg"

    fun credit(slug: String): PhotoCredit? = credits[slug]

    private val credits: Map<String, PhotoCredit> = mapOf(
        "australian_king_parrot" to PhotoCredit("JJ Harrison", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Australian_King_Parrot_male_4737.jpg"),
        "australian_magpie" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Cracticus_tibicen_hypoleuca_male_domain.jpg"),
        "australian_raven" to PhotoCredit("JJ Harrison (https://tiny.jjharrison.com.au/t/ZoQvcc05qhmjQ9eE)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Corvus_coronoides_-_Doughboy_Head.jpg"),
        "black_faced_cuckoo_shrike" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Coracina_novaehollandiae.jpg"),
        "common_blackbird" to PhotoCredit("Andreas Trepte", "CC BY-SA 2.5", "https://commons.wikimedia.org/wiki/File:Common_Blackbird.jpg"),
        "common_myna" to PhotoCredit("Gerrie van Vuuren", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Acridotheres_tristis00.jpg"),
        "crested_pigeon" to PhotoCredit("JJ Harrison (https://tiny.jjharrison.com.au/t/YgJqEWUNNzNFpTR5)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Ocyphaps_lophotes_-_Lake_Parramatta_Reserve.jpg"),
        "crimson_rosella" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Platycercus_elegans_Wilsons_Prom.jpg"),
        "eastern_spinebill" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Acanthorhynchus_tenuirostris_-_Mogo_Campground.jpg"),
        "eastern_yellow_robin" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Eopsaltria_australis_-_Mogo_Campground.jpg"),
        "galah" to PhotoCredit("Charles J. Sharp", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Galah_(Eolophus_roseicapilla_albiceps)_male_Adelaide.jpg"),
        "grey_butcherbird" to PhotoCredit("Tatiana Gerus", "CC BY 2.0", "https://commons.wikimedia.org/wiki/File:Cracticus_torquatus_-Brisbane_-garden_fence-8.jpg"),
        "grey_fantail" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Grey_Fantail_-_Brunkerville.jpg"),
        "house_sparrow" to PhotoCredit("Rhododendrites", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:House_sparrow_male_in_Prospect_Park_(53532).jpg"),
        "koel" to PhotoCredit("Aviceda", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Pacific_koel_male_kob08.JPG"),
        "laughing_kookaburra" to PhotoCredit("Photo by: Paulweberphoto - paulweberphoto.com", "CC BY 4.0", "https://commons.wikimedia.org/wiki/File:Laughing_Kookaburra_1.jpg"),
        "little_wattlebird" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Anthochaera_chrysoptera_4.jpg"),
        "magpie_lark" to PhotoCredit("Charles J. Sharp", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Magpie-lark_(Grallina_cyanoleuca_cyanoleuca)_male_Adelaide.jpg"),
        "new_holland_honeyeater" to PhotoCredit("Photo by: Paulweberphoto - paulweberphoto.com", "CC BY 4.0", "https://commons.wikimedia.org/wiki/File:New_Holland_honeyeater_().jpg"),
        "noisy_miner" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Manorina_melanocephala_AF.jpg"),
        "pied_butcherbird" to PhotoCredit("Vicki Nunn", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Pied_Butcherbird_-_Male_(Cracticus_nigrogularis)_03.JPG"),
        "pied_currawong" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Pied_Currawong_-_Newington.jpg"),
        "purple_swamphen" to PhotoCredit("Andreas Trepte", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Purpurhuhn.jpg"),
        "rainbow_lorikeet" to PhotoCredit("Charles J. Sharp", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Rainbow_lorikeet_(Trichoglossus_moluccanus_moluccanus)_Sydney.jpg"),
        "red_browed_finch" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Red-browed_Finch_-_Penrith.jpg"),
        "red_wattlebird" to PhotoCredit("Andreas Trepte", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Red_wattlebird.jpg"),
        "red_whiskered_bulbul" to PhotoCredit("Uploader1234567890", "CC0", "https://commons.wikimedia.org/wiki/File:Red-whiskered_bulbul_-_Pycnonotous_jocosus.png"),
        "rock_dove" to PhotoCredit("william_stephens", "CC BY 4.0", "https://commons.wikimedia.org/wiki/File:Columba_livia_(Rock_Dove,_wild),_Duncansby_Head,_Caithness,_Scotland_1.jpg"),
        "silvereye" to PhotoCredit("JJ Harrison", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Silvereye.jpg"),
        "spotted_pardalote" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Pardalotus_punctatus_2_-_Glen_Davis.jpg"),
        "spotted_turtle_dove" to PhotoCredit("Charles J. Sharp", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Spotted_dove_(Spilopelia_chinensis_suratensis).jpg"),
        "sulphur_crested_cockatoo" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Cacatua_galerita_Tas_2.jpg"),
        "superb_fairy_wren" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Malurus_cyaneus_cyanochlamys_.jpg"),
        "welcome_swallow" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 3.0", "https://commons.wikimedia.org/wiki/File:Hirundo_neoxena_risdon.jpg"),
        "willie_wagtail" to PhotoCredit("JJ Harrison (https://www.jjharrison.com.au/)", "CC BY-SA 4.0", "https://commons.wikimedia.org/wiki/File:Rhipidura_leucophrys_-_Glen_Davis.jpg"),
        "yellow_tailed_black_cockatoo" to PhotoCredit("David Cook Wildlife Photography", "CC BY-SA 2.0", "https://commons.wikimedia.org/wiki/File:Calyptorhynchus_funereus_(male)_-Wamboin-8.jpg"),
    )
}
