// eslint-disable-next-line @typescript-eslint/no-var-requires
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
  experimental: {
    optimizePackageImports: ['nextra'],
  },
})
