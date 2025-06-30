import type { AppProps } from 'next/app'
import type { JSX } from 'react'

export default function MyApp({ Component, pageProps }: AppProps): JSX.Element {
  return <Component {...pageProps} />
}
