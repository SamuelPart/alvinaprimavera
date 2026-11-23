import type { Config } from 'tailwindcss';

export default {
  darkMode: ['class'],
  content: ['./index.html', './carta-rapida.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        /* ── Marca Alviña ── */
        ink: '#140C07',
        espresso: '#2E1B0F',
        coffee: '#5A3A24',
        gold: { DEFAULT: '#C9A227', soft: '#E7D3A1', deep: '#9C7A18' },
        cream: { DEFAULT: '#FAF5EC', deep: '#F1E7D6', line: '#E4D6C0' },
        paper: '#FFFDF9',
        wa: { 500: '#1FA855', 600: '#178A45', 700: '#106E36' },

        /* ── Tokens shadcn/ui ── */
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: { DEFAULT: 'hsl(var(--primary))', foreground: 'hsl(var(--primary-foreground))' },
        secondary: { DEFAULT: 'hsl(var(--secondary))', foreground: 'hsl(var(--secondary-foreground))' },
        muted: { DEFAULT: 'hsl(var(--muted))', foreground: 'hsl(var(--muted-foreground))' },
        accent: { DEFAULT: 'hsl(var(--accent))', foreground: 'hsl(var(--accent-foreground))' },
        destructive: { DEFAULT: 'hsl(var(--destructive))', foreground: 'hsl(var(--destructive-foreground))' },
        popover: { DEFAULT: 'hsl(var(--popover))', foreground: 'hsl(var(--popover-foreground))' },
        card: { DEFAULT: 'hsl(var(--card))', foreground: 'hsl(var(--card-foreground))' },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
      fontFamily: {
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['Manrope', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        soft: '0 18px 40px -28px rgba(46,27,15,.55)',
        card: '0 2px 4px -2px rgba(46,27,15,.10), 0 18px 34px -26px rgba(46,27,15,.45)',
        lift: '0 26px 50px -30px rgba(46,27,15,.65)',
        bar: '0 24px 60px -24px rgba(20,12,7,.85)',
        sheet: '0 -30px 80px -30px rgba(20,12,7,.55)',
      },
    },
  },
  plugins: [],
} satisfies Config;
