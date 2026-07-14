---
name: Midnight Intelligence
colors:
  surface: '#051424'
  surface-dim: '#051424'
  surface-bright: '#2c3a4c'
  surface-container-lowest: '#010f1f'
  surface-container-low: '#0d1c2d'
  surface-container: '#122131'
  surface-container-high: '#1c2b3c'
  surface-container-highest: '#273647'
  on-surface: '#d4e4fa'
  on-surface-variant: '#c6c6cd'
  inverse-surface: '#d4e4fa'
  inverse-on-surface: '#233143'
  outline: '#909097'
  outline-variant: '#45464d'
  surface-tint: '#bec6e0'
  primary: '#bec6e0'
  on-primary: '#283044'
  primary-container: '#0f172a'
  on-primary-container: '#798098'
  inverse-primary: '#565e74'
  secondary: '#d2bbff'
  on-secondary: '#3f008e'
  secondary-container: '#6001d1'
  on-secondary-container: '#c9aeff'
  tertiary: '#89ceff'
  on-tertiary: '#00344d'
  tertiary-container: '#001a29'
  on-tertiary-container: '#0089c3'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dae2fd'
  primary-fixed-dim: '#bec6e0'
  on-primary-fixed: '#131b2e'
  on-primary-fixed-variant: '#3f465c'
  secondary-fixed: '#eaddff'
  secondary-fixed-dim: '#d2bbff'
  on-secondary-fixed: '#25005a'
  on-secondary-fixed-variant: '#5a00c6'
  tertiary-fixed: '#c9e6ff'
  tertiary-fixed-dim: '#89ceff'
  on-tertiary-fixed: '#001e2f'
  on-tertiary-fixed-variant: '#004c6e'
  background: '#051424'
  on-background: '#d4e4fa'
  surface-variant: '#273647'
typography:
  display-lg:
    fontFamily: hankenGrotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: hankenGrotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: hankenGrotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
  body-md:
    fontFamily: inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: geist
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  unit: 4px
  margin-mobile: 20px
  margin-desktop: 40px
  gutter: 16px
  stack-sm: 8px
  stack-md: 24px
  stack-lg: 48px
---

## Brand & Style

The design system is anchored in the concept of "Quiet Power." It is a premium, AI-centric interface that prioritizes calm interaction and futuristic elegance. Designed for a high-end mobile experience, it utilizes a sophisticated dark mode aesthetic that feels expansive and immersive.

The style is **Modern Glassmorphism** with a focus on depth and luminosity. It leverages frosted glass effects, vibrant background blurs, and precise high-fidelity details to signal intelligence and accessibility. The UI should evoke an emotional response of trust, clarity, and cutting-edge sophistication, similar to high-end hardware-software integration.

## Colors

The palette is centered on a "Midnight" foundation, using deep slate and navy tones to create a sense of infinite depth.

- **Primary (Midnight):** `#0F172A` — Used for the core background surfaces to ensure maximum contrast for AI states.
- **Secondary (Electric Violet):** `#7C3AED` — The primary "AI Thinking" color, representing intelligence and creativity.
- **Tertiary (Azure):** `#0EA5E9` — Used for "Listening" states and interactive actions, providing a calm, tech-forward energy.
- **Neutral (Slate Gray):** `#94A3B8` — Used for secondary text and subtle borders to maintain a sophisticated hierarchy.

Accent colors should be used with a "glow" treatment—incorporating subtle outer glows or background blurs to simulate light emitting from the interface.

## Typography

This design system uses a trio of sans-serif fonts to achieve a technical yet premium feel. **Hanken Grotesk** provides a sharp, contemporary look for large headlines. **Inter** is utilized for body text to ensure maximum readability and a neutral, professional tone. **Geist** is reserved for labels and technical data, providing a developer-precise aesthetic that reinforces the AI theme.

Spacing is generous. Headlines should use tighter letter-spacing to feel "locked-in" and intentional, while labels use expanded tracking for a refined, modern look.

## Layout & Spacing

The layout follows a **Fluid Grid** model with a heavy emphasis on safe areas and vertical rhythm. 

- **Mobile:** A 4-column grid with 20px side margins. AI "Orbs" or voice visualizers are centered or anchored to the bottom 30% of the screen.
- **Desktop/Tablet:** A 12-column grid. Content is centered in a max-width container of 1200px to maintain focus.

Spacing is derived from a 4px base unit. Generous "breathing room" is mandatory—use `stack-lg` between major functional sections to prevent the UI from feeling cluttered. The interface should feel like it "floats" within the Midnight void.

## Elevation & Depth

Depth is communicated through **Glassmorphism** and **Luminous Layers** rather than traditional shadows.

1.  **Background:** The base Midnight color.
2.  **Surface-Low:** A slightly lighter slate (`#1E293B`) with 0% opacity backgrounds and a 1px stroke (10% white).
3.  **Surface-Glass:** Semi-transparent layers (Blur: 40px, Opacity: 15-20%) that allow the background AI "glows" to bleed through.
4.  **Floating Elements:** Elements that require the most attention use an "Ambient Glow"—a soft, colored drop shadow matching the element's primary accent (Violet or Azure) at 15% opacity with a 60px blur.

Outlines are "Ghost Borders"—ultra-thin 1px strokes with 10-15% white opacity to define shape without adding visual weight.

## Shapes

The shape language is extremely organic and soft. Following premium hardware trends, corner radii are large and inviting.

- **Standard Cards/Containers:** 24px minimum radius.
- **Interactive Buttons:** 32px or full-pill (rounded-xl/full).
- **AI Visualizers:** Perfectly circular or amorphous fluid shapes with high-velocity motion.

Sharp corners are strictly avoided to maintain the "approachable future" aesthetic.

## Components

### Buttons
Primary buttons use a "Luminous Fill"—a gradient of Azure to Electric Violet with a subtle white inner glow at the top edge. Secondary buttons use the Glassmorphism style (backplate blur with a thin white stroke).

### AI Orbs & Visualizers
The central component of the design system. This should be a fluid, animated gradient sphere that reacts to voice frequency. It uses a high backdrop blur to soften the transition between the orb and the Midnight background.

### Cards
Cards are "Frosted Glass" panes. They should not have a solid background color; instead, use a blur filter and a subtle 1px border. Content inside cards should be left-aligned with generous 24px internal padding.

### Inputs
Input fields are simplified to a single bottom stroke or a very subtle glass container. When focused, the stroke glows with the Electric Violet primary accent.

### Chips/Tags
Small, pill-shaped elements with 12px vertical padding. They act as "Quick Actions" for the AI, using the `label-md` typography style for a technical feel.

### Lists
Lists are "borderless," separated by whitespace or extremely faint 1px lines that fade out toward the edges of the screen.