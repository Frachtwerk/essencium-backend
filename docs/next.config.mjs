import nextra from 'nextra'

const withNextra = nextra({
  latex: true,
  search: {
    codeblocks: false,
  },
})

export default withNextra({
  reactStrictMode: true,
  pageExtensions: ['ts', 'tsx', 'md', 'mdx'],
  output: 'export',
  images: {
    unoptimized: true,
  },
  // Wenn GitHub Pages unter einem Subpath läuft (z.B. /essencium-backend/)
  basePath: '/essencium-backend',
  assetPrefix: '/essencium-backend/',
})
