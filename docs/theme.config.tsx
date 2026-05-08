import { DocsThemeConfig } from 'nextra-theme-docs'
import { useRouter } from 'next/router'
import React from 'react'

const config: DocsThemeConfig = {
  logo: function Logo() {
    const { basePath } = useRouter()
    return (
      <img
        src={`${basePath}/logotype_400x100px.svg`}
        alt="Essencium Logo"
        width="150px"
        height="auto"
      />
    )
  },
  head: function useHead() {
    const { basePath } = useRouter()
    return (
      <>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta httpEquiv="Content-Language" content="en" />
        <link rel="icon" href={`${basePath}/favicon.ico`} sizes="any" />
      </>
    )
  },
  project: {
    link: 'https://github.com/Frachtwerk/essencium-backend',
  },
  docsRepositoryBase:
    'https://github.com/Frachtwerk/essencium-backend/tree/main/docs',
  footer: {
    content: <span>Essencium Docs</span>,
  },
}

export default config
