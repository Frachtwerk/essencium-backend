import nextra from 'nextra'

const withNextra = nextra({
  theme: 'nextra-theme-docs',
  themeConfig: './theme.config.tsx',
  latex: true,
  search: {
    codeblocks: false,
  },
})

export default withNextra({
  reactStrictMode: true,
  output: 'export',
  images: {
    unoptimized: true,
  },
  // Wenn GitHub Pages unter einem Subpath läuft (z.B. /essencium-backend/)
  basePath: '/essencium-backend',
  assetPrefix: '/essencium-backend/',
})
