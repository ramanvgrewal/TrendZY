/**
 * ═══════════════════════════════════════════════════════════
 *  TrendZY — Smart Product Type Classifier
 *  Detects the actual WEAR TYPE from product name + fields.
 *  Priority: subcategory → productName keywords → category
 * ═══════════════════════════════════════════════════════════
 */

const TYPE_MAP = [
    // ── FOOTWEAR ──────────────────────────────────────────────
    {
        key: 'sneakers',
        label: 'Sneakers',
        emoji: '👟',
        accent: '#a3e635',
        dim: 'rgba(163,230,53,0.10)',
        keywords: [
            'sneaker','gazelle','air force','air max','jordan','dunk','yeezy',
            'chuck','converse','vans','new balance','nb 550','nb 574','adidas',
            'puma suede','reebok','samba','spezial','campus',
        ],
    },
    {
        key: 'boots',
        label: 'Boots',
        emoji: '🥾',
        accent: '#d97706',
        dim: 'rgba(217,119,6,0.10)',
        keywords: [
            'boot','dr martens','martens','chelsea boot','ankle boot','combat boot',
            'timberland','ugg',
        ],
    },
    {
        key: 'loafers',
        label: 'Loafers & Flats',
        emoji: '👞',
        accent: '#fbbf24',
        dim: 'rgba(251,191,36,0.10)',
        keywords: ['loafer','flat','mule','mary jane','slip-on','slip on','ballet flat'],
    },
    {
        key: 'sandals',
        label: 'Sandals & Slides',
        emoji: '🩴',
        accent: '#34d399',
        dim: 'rgba(52,211,153,0.10)',
        keywords: ['sandal','slide','birkenstock','chappal','slipper','flip flop','flip-flop'],
    },

    // ── TOPS ──────────────────────────────────────────────────
    {
        key: 'tees',
        label: 'T-Shirts & Tees',
        emoji: '👕',
        accent: '#fbbf24',
        dim: 'rgba(251,191,36,0.10)',
        keywords: [
            't-shirt','tee','tshirt','graphic tee','oversized tee','boxy tee',
            'crop tee','jersey','mesh jersey','band tee','printed tee',
        ],
    },
    {
        key: 'shirts',
        label: 'Shirts',
        emoji: '👔',
        accent: '#60a5fa',
        dim: 'rgba(96,165,250,0.10)',
        keywords: [
            'shirt','button','linen shirt','flannel','oxford','cuban collar',
            'camp collar','hawaiian','bowling shirt','overshirt',
        ],
    },
    {
        key: 'tops',
        label: 'Tops & Blouses',
        emoji: '🫧',
        accent: '#f472b6',
        dim: 'rgba(244,114,182,0.10)',
        keywords: [
            'top','blouse','corset','cami','camisole','tank','tube top',
            'halter','bralette','crop top','off shoulder','peplum',
        ],
    },
    {
        key: 'kurta',
        label: 'Kurtas & Ethnics',
        emoji: '🌿',
        accent: '#6ee7b7',
        dim: 'rgba(110,231,183,0.10)',
        keywords: [
            'kurta','kurti','salwar','churidar','anarkali','lehenga','dupatta',
            'ethnic','block print','hand-block','ikat','bandhani','handloom',
            'khadi','saree','sari',
        ],
    },

    // ── HOODIES & SWEATSHIRTS ─────────────────────────────────
    {
        key: 'hoodies',
        label: 'Hoodies & Sweatshirts',
        emoji: '🧥',
        accent: '#fb923c',
        dim: 'rgba(251,146,60,0.10)',
        keywords: [
            'hoodie','hoody','sweatshirt','pullover','fleece','zip-up','zip up',
            'oversized hoodie','college sweat','crewneck','crew neck',
        ],
    },

    // ── OUTERWEAR ─────────────────────────────────────────────
    {
        key: 'jackets',
        label: 'Jackets',
        emoji: '🫶',
        accent: '#f97316',
        dim: 'rgba(249,115,22,0.10)',
        keywords: [
            'jacket','bomber','denim jacket','varsity','windbreaker','puffer',
            'trench','leather jacket','biker','coach jacket','trucker',
        ],
    },
    {
        key: 'shackets',
        label: 'Shackets & Overshirts',
        emoji: '🍂',
        accent: '#d97706',
        dim: 'rgba(217,119,6,0.10)',
        keywords: ['shacket','overshirt','corduroy jacket','flannel jacket','quilted'],
    },
    {
        key: 'coats',
        label: 'Coats & Blazers',
        emoji: '🎩',
        accent: '#94a3b8',
        dim: 'rgba(148,163,184,0.10)',
        keywords: ['coat','blazer','overcoat','trench coat','double breasted'],
    },

    // ── BOTTOMS ───────────────────────────────────────────────
    {
        key: 'jeans',
        label: 'Denim & Jeans',
        emoji: '👖',
        accent: '#818cf8',
        dim: 'rgba(129,140,248,0.10)',
        keywords: [
            'jean','denim','501','wide leg denim','straight cut denim',
            'baggy denim','raw denim','distressed denim','mom jean','dad jean',
            'bootcut','skinny jean',
        ],
    },
    {
        key: 'cargo',
        label: 'Cargo Pants',
        emoji: '🪖',
        accent: '#a3e635',
        dim: 'rgba(163,230,53,0.10)',
        keywords: [
            'cargo','baggy cargo','cargo pant','utility pant','parachute pant',
            'tactical pant',
        ],
    },
    {
        key: 'trousers',
        label: 'Trousers & Chinos',
        emoji: '👔',
        accent: '#c4b5fd',
        dim: 'rgba(196,181,253,0.10)',
        keywords: ['trouser','chino','slacks','wide leg trouser','palazzo','linen pant','dress pant'],
    },
    {
        key: 'shorts',
        label: 'Shorts',
        emoji: '🩳',
        accent: '#34d399',
        dim: 'rgba(52,211,153,0.10)',
        keywords: ['short','bermuda','cycling short','biker short','mini short'],
    },
    {
        key: 'skirts',
        label: 'Skirts & Minis',
        emoji: '🌷',
        accent: '#f9a8d4',
        dim: 'rgba(249,168,212,0.10)',
        keywords: ['skirt','mini skirt','maxi skirt','midi skirt','wrap skirt','pleated skirt'],
    },

    // ── CO-ORDS & DRESSES ─────────────────────────────────────
    {
        key: 'coords',
        label: 'Co-ord Sets',
        emoji: '🪡',
        accent: '#2dd4bf',
        dim: 'rgba(45,212,191,0.10)',
        keywords: [
            'co-ord','coord','set','matching set','two piece','two-piece',
            'linen set','satin set','track set',
        ],
    },
    {
        key: 'dresses',
        label: 'Dresses',
        emoji: '👗',
        accent: '#f472b6',
        dim: 'rgba(244,114,182,0.10)',
        keywords: [
            'dress','sundress','slip dress','maxi dress','midi dress',
            'mini dress','wrap dress','bodycon','floral dress',
        ],
    },

    // ── BEAUTY & SKINCARE ─────────────────────────────────────
    {
        key: 'serums',
        label: 'Serums & Skincare',
        emoji: '🧴',
        accent: '#f472b6',
        dim: 'rgba(244,114,182,0.10)',
        keywords: [
            'serum','toner','moisturiser','moisturizer','spf','sunscreen',
            'retinol','niacinamide','vitamin c','hyaluronic','face wash',
            'cleanser','face cream','eye cream','skin tint','tinted',
            'glass skin','skincare kit','skin kit',
        ],
    },
    {
        key: 'makeup',
        label: 'Makeup',
        emoji: '💄',
        accent: '#fb7185',
        dim: 'rgba(251,113,133,0.10)',
        keywords: [
            'lipstick','lip gloss','lip liner','foundation','concealer',
            'blush','bronzer','highlighter','eyeshadow','mascara','eyeliner',
            'kajal','contour','primer','setting spray','bb cream','cc cream',
            'cushion','powder','makeup',
        ],
    },
    {
        key: 'haircare',
        label: 'Hair & Body',
        emoji: '💆',
        accent: '#c084fc',
        dim: 'rgba(192,132,252,0.10)',
        keywords: [
            'shampoo','conditioner','hair oil','hair mask','hair serum',
            'hair cream','dry shampoo','body wash','body lotion','body butter',
            'fragrance','perfume','deodorant','hair care',
        ],
    },

    // ── ACCESSORIES ───────────────────────────────────────────
    {
        key: 'bags',
        label: 'Bags & Totes',
        emoji: '👜',
        accent: '#fbbf24',
        dim: 'rgba(251,191,36,0.10)',
        keywords: [
            'bag','tote','backpack','sling bag','crossbody','handbag','clutch',
            'canvas bag','mesh bag','denim bag','upcycled','mini bag',
        ],
    },
    {
        key: 'caps',
        label: 'Caps & Hats',
        emoji: '🧢',
        accent: '#a3e635',
        dim: 'rgba(163,230,53,0.10)',
        keywords: [
            'cap','hat','bucket hat','beanie','snapback','dad cap','baseball cap',
            'trucker hat','crochet hat','beret',
        ],
    },
    {
        key: 'jewelry',
        label: 'Jewelry',
        emoji: '💍',
        accent: '#c4b5fd',
        dim: 'rgba(196,181,253,0.10)',
        keywords: [
            'ring','necklace','chain','bracelet','anklet','earring','stud',
            'hoop','pendant','choker','layered chain','silver ring',
            'gold chain','jewelry','jewellery','charm',
        ],
    },
    {
        key: 'hairclips',
        label: 'Hair Accessories',
        emoji: '🎀',
        accent: '#f9a8d4',
        dim: 'rgba(249,168,212,0.10)',
        keywords: [
            'hair clip','hairclip','butterfly clip','claw clip','scrunchie',
            'hair band','hair tie','headband','barrette',
        ],
    },
    {
        key: 'sunglasses',
        label: 'Sunglasses & Eyewear',
        emoji: '🕶️',
        accent: '#fcd34d',
        dim: 'rgba(252,211,77,0.10)',
        keywords: ['sunglass','glasses','eyewear','shades','tinted glasses','cat eye'],
    },
    {
        key: 'scarves',
        label: 'Scarves & Wraps',
        emoji: '🧣',
        accent: '#6ee7b7',
        dim: 'rgba(110,231,183,0.10)',
        keywords: ['scarf','wrap','stole','shawl','dupatta scarf','bandana','handwoven scarf','ikat scarf'],
    },
    {
        key: 'belts',
        label: 'Belts & Wallets',
        emoji: '🪬',
        accent: '#d97706',
        dim: 'rgba(217,119,6,0.10)',
        keywords: ['belt','wallet','card holder','money clip'],
    },

    // ── TECH & LIFESTYLE ──────────────────────────────────────
    {
        key: 'tech',
        label: 'Tech & Gadgets',
        emoji: '📱',
        accent: '#60a5fa',
        dim: 'rgba(96,165,250,0.10)',
        keywords: [
            'phone','earphone','earbud','headphone','smartwatch','charger',
            'cable','laptop','tablet','speaker','gadget','tech',
        ],
    },
    {
        key: 'lifestyle',
        label: 'Lifestyle',
        emoji: '✨',
        accent: '#a78bfa',
        dim: 'rgba(167,139,250,0.10)',
        keywords: ['candle','diffuser','journal','notebook','mug','bottle','flask','poster','decor'],
    },
];

const DEFAULT_TYPE = {
    key: 'other',
    label: 'Other',
    emoji: '✦',
    accent: '#737373',
    dim: 'rgba(115,115,115,0.10)',
};

/**
 * Detects product type config from a product object.
 * Priority: subcategory field → productName keyword scan → category field
 */
export function detectProductType(product) {
    const name     = (product.productName || '').toLowerCase();
    const subcat   = (product.subcategory  || '').toLowerCase();
    const cat      = (product.category     || '').toLowerCase();
    const combined = `${name} ${subcat} ${cat}`;

    // 1. Check subcategory first (most specific)
    for (const type of TYPE_MAP) {
        for (const kw of type.keywords) {
            if (subcat.includes(kw)) return type;
        }
    }

    // 2. Scan productName (longest keyword match wins → "cargo pant" beats "pant")
    let bestMatch = null;
    let bestLen   = 0;
    for (const type of TYPE_MAP) {
        for (const kw of type.keywords) {
            if (combined.includes(kw) && kw.length > bestLen) {
                bestMatch = type;
                bestLen   = kw.length;
            }
        }
    }
    if (bestMatch) return bestMatch;

    // 3. Fall back to category-level match
    for (const type of TYPE_MAP) {
        if (type.keywords.some((kw) => cat.includes(kw))) return type;
    }

    // 4. Return "Other" with label = category string if available
    if (cat) return { ...DEFAULT_TYPE, label: cat.charAt(0).toUpperCase() + cat.slice(1) };
    return DEFAULT_TYPE;
}

/**
 * Groups an array of products by detected product type.
 * Returns: Array of [typeConfig, products[]] sorted by count desc.
 */
export function groupByProductType(data) {
    const arr = data?.content ?? (Array.isArray(data) ? data : []);
    const map  = {};   // key → { cfg, products[] }

    arr.forEach((p) => {
        const cfg = detectProductType(p);
        if (!map[cfg.key]) map[cfg.key] = { cfg, products: [] };
        map[cfg.key].products.push(p);
    });

    return Object.values(map)
        .sort((a, b) => b.products.length - a.products.length);
}