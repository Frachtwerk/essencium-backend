# Documentation using Nextra

## Local Development

To run the documentation locally, run:

```bash
    curl https://get.volta.sh | bash # or install a node version manager (nvm) of you choice
    cd <repo>/docs
    pnpm install
    pnpm dev
    visit http://localhost:3000
```

## Building

To build the documentation for production:

```bash
    cd <repo>/docs
    pnpm build
```

This creates a static export in the `out/` directory.

## Deployment

The documentation is automatically built and deployed to GitHub Pages via GitHub Actions when changes are pushed to the `main` or `master` branch.

For detailed deployment instructions and configuration, see [DEPLOYMENT.md](./DEPLOYMENT.md).
