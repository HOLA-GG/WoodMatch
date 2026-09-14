---
name: Arboreal Organic
colors:
  surface: '#fff8f3'
  surface-dim: '#e0d9d3'
  surface-bright: '#fff8f3'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#faf2ec'
  surface-container: '#f4ede6'
  surface-container-high: '#eee7e1'
  surface-container-highest: '#e8e1db'
  on-surface: '#1e1b17'
  on-surface-variant: '#414844'
  inverse-surface: '#33302c'
  inverse-on-surface: '#f7efe9'
  outline: '#717973'
  outline-variant: '#c1c8c2'
  surface-tint: '#3f6653'
  primary: '#012d1d'
  on-primary: '#ffffff'
  primary-container: '#1b4332'
  on-primary-container: '#86af99'
  inverse-primary: '#a5d0b9'
  secondary: '#4c6452'
  on-secondary: '#ffffff'
  secondary-container: '#cce6d0'
  on-secondary-container: '#506856'
  tertiary: '#421b00'
  on-tertiary: '#ffffff'
  tertiary-container: '#632c00'
  on-tertiary-container: '#ff8529'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c1ecd4'
  primary-fixed-dim: '#a5d0b9'
  on-primary-fixed: '#002114'
  on-primary-fixed-variant: '#274e3d'
  secondary-fixed: '#cee9d3'
  secondary-fixed-dim: '#b3cdb7'
  on-secondary-fixed: '#092012'
  on-secondary-fixed-variant: '#354c3b'
  tertiary-fixed: '#ffdbc8'
  tertiary-fixed-dim: '#ffb68a'
  on-tertiary-fixed: '#321300'
  on-tertiary-fixed-variant: '#743500'
  background: '#fff8f3'
  on-background: '#1e1b17'
  surface-variant: '#e8e1db'
typography:
  display-lg:
    fontFamily: Newsreader
    fontSize: 48px
    fontWeight: '600'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '600'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 64px
  container-max: 1280px
---

## Brand & Style

The design system is rooted in the "Arboreal Organic" philosophy—a visual language that bridges high-utility technology with the serene, cyclical nature of the forest. The target audience includes conservationists, arborists, and eco-conscious citizens who require an interface that feels as grounded and life-giving as the ecosystems they protect.

The style is a synthesis of **Organic Minimalism** and **Soft Glassmorphism**. It utilizes heavy whitespace to provide "breathable" layouts, reminiscent of forest clearings. High-contrast elements ensure professional-grade accessibility, while translucent layers and soft-focus background blurs mimic the depth and dappled light found under a tree canopy. The emotional response is one of calm, reliability, and biological connection.

## Colors

The palette is dynamic and seasonal, shifting to reflect the natural world. While the core tokens represent a "Deep Forest" (Summer) baseline, the system supports seasonal overrides:

*   **Summer (Default):** Deep Forest Greens (#1B4332) for primary actions, Vibrant Leaf Greens (#74C69D) for accents.
*   **Autumn:** Burnt Orange (#BC4749) and Earthy Browns (#7F5539) for high-contrast alerts and status indicators.
*   **Spring:** Blossom Pinks (#F28482) and Fresh Sprout Greens (#95D5B2) for soft background surfaces.
*   **Winter:** Slate Greys (#4A4E69) and Pine Needle Dark Green (#2D6A4F) for a more somber, focused tone.

The default background across all seasons is an off-white "Parchment" (#F8F9F5) to reduce eye strain and maintain a natural, non-synthetic feel.

## Typography

This design system employs a "Field Guide" typographic hierarchy. 

**Newsreader** is utilized for all headings to provide an authoritative, literary feel. It carries an organic, slightly irregular rhythm that mimics classic botanical texts. **Inter** is the functional workhorse for all body copy, data, and interface labels, ensuring maximum readability and a clean, modern contrast against the serif headlines. 

Text should never be pure black; instead, use a "Bark Brown" (#2D2A26) to maintain softness while keeping contrast ratios above 7:1 for accessibility.

## Layout & Spacing

The layout follows a **Fluid Content Grid** modeled after natural growth patterns—structured but not rigid. 

1.  **Grid System:** A 12-column grid for desktop with wide 24px gutters to prevent content crowding.
2.  **Rhythm:** An 8px base unit drives all spacing. Use generous padding (32px+) inside containers to evoke a sense of openness.
3.  **Adaptation:** On mobile, margins tighten to 16px, and multi-column card layouts reflow into a single vertical "stack" to mimic a narrow trail or path.
4.  **Asymmetry:** Occasionally break the grid with "Organic Insets"—elements like images or pull-quotes that sit 50% off-center to mimic the unpredictable placement of trees in a landscape.

## Elevation & Depth

Visual hierarchy in this design system is achieved through **Tonal Stacking** and **Glassmorphism**, rather than traditional heavy shadows.

*   **The Ground Plane:** The base parchment layer is the lowest elevation.
*   **The Canopy (Glassmorphism):** Overlays, modals, and navigation bars use a backdrop blur (20px) with a semi-transparent white tint (70% opacity). This creates a "frosted leaf" effect where background colors softly bleed through.
*   **Shadows:** When necessary, use "Ambient Earth Shadows"—extremely soft, diffused blurs (30px radius, 5% opacity) tinted with a deep forest green instead of grey to maintain a warm, natural depth.
*   **Outlines:** Low-contrast borders (1px, 10% opacity Bark Brown) define secondary elements without creating harsh visual breaks.

## Shapes

The shape language is strictly **Pill-shaped and Organic**. Harsh 90-degree angles are avoided to reflect the lack of straight lines in nature. 

*   **Primary Containers:** Use a `rounded-xl` (1.5rem / 24px) or higher to create soft, pebble-like surfaces.
*   **Interactive Elements:** Buttons and tags use a full pill shape (999px) for a tactile, friendly appearance.
*   **Organic Masks:** Image assets should occasionally use non-uniform "blob" masks (smooth, varying radii) to simulate the shapes of leaves or cross-sections of timber.

## Components

*   **Buttons:** High-contrast Forest Green backgrounds with white Inter-medium text. State changes (hover/active) should involve a subtle shift to a warmer green or a slight "elevation lift" via increased blur.
*   **Cards:** Glassmorphic containers with 24px internal padding. Headers within cards use Newsreader, while metadata uses Inter-label-sm in a muted Earth Brown.
*   **Inputs:** Minimalist fields with a soft bottom-border or a very light recessed fill. Focus states utilize a vibrant Sprout Green "glow" (2px soft border).
*   **Chips/Tags:** Rounded pill shapes used for tree species or climate zones. Use seasonal colors (e.g., a "Maple" tag in Autumn Red).
*   **Progress Indicators:** Represented as a "growing branch" or a filling leaf icon rather than a standard linear bar, adding a playful, thematic touch to data loading or goal tracking.
*   **Lists:** Separated by "Hairline Roots"—thin, low-opacity dividers that terminate before hitting the container edges.