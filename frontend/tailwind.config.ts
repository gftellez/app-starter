import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        // A distinct display face for headings, kept separate from body text.
        display: ['"Space Grotesk"', 'Inter', 'sans-serif'],
        // A monospace face for anything tabular or code-like.
        mono:    ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      colors: {
        // Placeholder palette. This is the FIRST thing a new app should
        // replace: pick real hues for `background`/`surface`/`accent`/`ink`
        // and, if the app has its own colour-coded categories the way the
        // reference app coded currencies, a validated set of `mark` colors.
        //
        // Values live in src/index.css as RGB channels (not hex/named colors)
        // so Tailwind's `/opacity` modifiers keep working (`bg-accent/10`)
        // and so light/dark can swap the values without touching markup.
        background: 'rgb(var(--background) / <alpha-value>)',
        surface:    'rgb(var(--surface) / <alpha-value>)',
        chrome:     'rgb(var(--chrome) / <alpha-value>)',
        ink: {
          DEFAULT: 'rgb(var(--ink) / <alpha-value>)',
          soft:    'rgb(var(--ink-soft) / <alpha-value>)',
          mute:    'rgb(var(--ink-mute) / <alpha-value>)',
          faint:   'rgb(var(--ink-faint) / <alpha-value>)',
        },
        rule:   'rgb(var(--rule) / <alpha-value>)',
        accent: { DEFAULT: 'rgb(var(--accent) / <alpha-value>)', hover: 'rgb(var(--accent-hover) / <alpha-value>)' },
        // Semantic sign colors — for anything that reads as "good"/"bad".
        pos: 'rgb(var(--pos) / <alpha-value>)',
        neg: 'rgb(var(--neg) / <alpha-value>)',
      },
      boxShadow: {
        card: '0 1px 2px rgb(0 0 0 / 0.04)',
        lift: '0 6px 20px -10px rgb(0 0 0 / 0.18)',
      },
      borderRadius: { xl2: '0.875rem' },
      keyframes: {
        'fade-in-up': {
          '0%':   { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-in': { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) both',
        'fade-in':    'fade-in 0.3s ease-out both',
      },
    },
  },
  plugins: [],
}
export default config
