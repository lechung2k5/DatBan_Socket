---
name: Tinh Hoa Management
colors:
  surface: '#fcf9f3'
  surface-dim: '#dcdad4'
  surface-bright: '#fcf9f3'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f6f3ed'
  surface-container: '#f0eee8'
  surface-container-high: '#ebe8e2'
  surface-container-highest: '#e5e2dc'
  on-surface: '#1c1c18'
  on-surface-variant: '#59413f'
  inverse-surface: '#31312d'
  inverse-on-surface: '#f3f0ea'
  outline: '#8d706e'
  outline-variant: '#e1bebc'
  surface-tint: '#b2292d'
  primary: '#6b000d'
  on-primary: '#ffffff'
  primary-container: '#920d1a'
  on-primary-container: '#ff9c96'
  inverse-primary: '#ffb3ae'
  secondary: '#775a19'
  on-secondary: '#ffffff'
  secondary-container: '#fed488'
  on-secondary-container: '#785a1a'
  tertiary: '#323232'
  on-tertiary: '#ffffff'
  tertiary-container: '#484848'
  on-tertiary-container: '#b8b7b6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad7'
  primary-fixed-dim: '#ffb3ae'
  on-primary-fixed: '#410005'
  on-primary-fixed-variant: '#900b19'
  secondary-fixed: '#ffdea5'
  secondary-fixed-dim: '#e9c176'
  on-secondary-fixed: '#261900'
  on-secondary-fixed-variant: '#5d4201'
  tertiary-fixed: '#e4e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1b1c1c'
  on-tertiary-fixed-variant: '#474747'
  background: '#fcf9f3'
  on-background: '#1c1c18'
  surface-variant: '#e5e2dc'
typography:
  h1:
    fontFamily: Be Vietnam Pro
    fontSize: 40px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  h2:
    fontFamily: Be Vietnam Pro
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.3'
  h3:
    fontFamily: Be Vietnam Pro
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.4'
  body-lg:
    fontFamily: Be Vietnam Pro
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-sm:
    fontFamily: Be Vietnam Pro
    fontSize: 14px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: 0.05em
  button:
    fontFamily: Be Vietnam Pro
    fontSize: 16px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 40px
  xl: 64px
  container-margin: 24px
  gutter: 16px
---

## Brand & Style

The design system is anchored in the concept of "Sự Sang Trọng Hiếu Khách" (Refined Hospitality). It bridges the gap between high-end operational efficiency and the warmth of Vietnamese culinary culture. The visual language is **Corporate Modern** with a **Minimalist** focus on whitespace to reduce cognitive load in fast-paced restaurant environments.

The system targets restaurant owners and managers in Vietnam who value prestige and clarity. The interface avoids cold, purely "tech" aesthetics in favor of a lifestyle-oriented approach that feels as curated as a fine-dining menu. Interactions should be smooth and intentional, evoking a sense of calm control.

## Colors

The palette is rooted in Vietnamese heritage symbols—ruby lacquer and gold leaf—reimagined for a digital interface.

- **Primary (Deep Ruby Red):** #920D1A. Used for primary actions, branding, and critical highlights. It conveys authority and passion.
- **Secondary (Elegant Gold):** #C5A059. Used sparingly for accents, rewards, premium status indicators, and delicate borders.
- **Surface (Warm Cream):** #F9F6F0. The primary background color to reduce eye strain and provide a "paper-like" tactile quality.
- **Contrast (Charcoal):** #2C2C2C. Used for typography and deep structural elements, providing a grounded, professional feel.
- **Success/Warning:** Use muted versions of emerald green and ochre to maintain the sophisticated tonal range.

## Typography

This design system utilizes **Be Vietnam Pro** exclusively. This font was specifically engineered for the Vietnamese language, ensuring that diacritics (dấu) do not clash with line heights or appear cramped. 

Headlines use tighter letter spacing and heavier weights to establish a clear hierarchy. Body text maintains a generous line height (1.6) to ensure legibility on mobile devices used in kitchens or at tablesides. Labels are often set in Medium weight for quick scanning of data points like table numbers or price totals.

## Layout & Spacing

The system employs a **Fixed Grid** for desktop dashboards and a **Fluid Grid** for mobile tablet interfaces. 

A 12-column system is used for wide screens, while a 4-column system is used for mobile. The layout is heavily **card-based**, where each functional area (e.g., "Bàn đang phục vụ" or "Doanh thu") is encapsulated in a distinct container. 

Spacing follows a strict 8px rhythmic scale. Use `md (24px)` for internal card padding to maintain a premium, airy feel. Gutters are set to `16px` to keep related information clusters tight but distinct.

## Elevation & Depth

Visual hierarchy is achieved through **Tonal Layers** and **Ambient Shadows**.

1.  **Canvas:** The base layer is the Neutral Cream (#F9F6F0).
2.  **Cards:** Elements sit on white (#FFFFFF) surfaces with a subtle, highly diffused shadow (RGBA 146, 13, 26, 0.04) to create a soft lift without looking "heavy."
3.  **Active States:** Selected items use a secondary gold border (1px) or a subtle ruby-tinted glow.
4.  **Modals:** Use a heavy backdrop blur (12px) to focus attention, reinforcing the premium, high-focus nature of the application.

## Shapes

The shape language is defined by a consistent **16px (1rem)** corner radius for all primary containers and cards. This large radius softens the "corporate" feel, making the app appear more approachable and modern.

- **Primary Containers:** 16px (rounded-lg)
- **Buttons & Inputs:** 12px (rounded-md) for a more precise, clickable feel.
- **Tags/Chips:** Fully rounded (pill) to contrast against the structured card layout.

## Components

- **Cards:** The core of the design system. Cards must have a white background, 16px corner radius, and 24px internal padding. Titles within cards should be in H3 (Ruby Red or Charcoal).
- **Buttons:** 
    - *Primary:* Deep Ruby background with White text.
    - *Secondary:* Ghost style with Gold border and text.
    - *Action:* Always 48px height for touch-friendly operation in busy environments.
- **Input Fields:** Use a subtle cream-to-white gradient or solid white with a 1px Charcoal-at-10% border. Floating labels are preferred to keep the "Vietnamese" context clear even when text is entered.
- **Status Chips:** Used for "Đã thanh toán" (Paid) or "Đang chờ" (Pending). Use rounded pill shapes with low-opacity background fills of the status color.
- **Table Grid:** For the floor plan, use the 16px rounded squares. Occupied tables use a Ruby Red fill; available tables use a Gold outline.
- **Navigation:** A clean side-rail (Charcoal background) on desktop or a bottom-bar (Cream background) on mobile, using Gold for the active icon state.