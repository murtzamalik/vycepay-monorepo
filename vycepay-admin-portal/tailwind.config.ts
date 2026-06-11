import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bg: { primary: '#070912', surface: '#0F1221', elevated: '#141828', sidebar: '#0B0D18' },
        brand: { DEFAULT: '#4F79FF', hover: '#3D63E6', dim: 'rgba(79,121,255,0.12)' },
        success: '#0FD67C',
        warning: '#F5A623',
        danger: '#FF3D57',
        purple: '#8B5CF6',
        border: { DEFAULT: '#1B2040', strong: '#252D50' },
        text: { primary: '#E4E7F2', secondary: '#6B7394', muted: '#3D4460' },
      },
      fontFamily: { sans: ['Inter', 'sans-serif'], mono: ['JetBrains Mono', 'monospace'] },
    },
  },
  plugins: [],
}
export default config
