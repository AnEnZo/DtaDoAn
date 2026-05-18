# Frontend Development Best Practices

A comprehensive guide to modern frontend development, compiled from authoritative sources and industry standards.

## Core Principles

### 1. Performance First
- **Web Vitals** (Google): Focus on Core Web Vitals (LCP, FID, CLS)
- **Lighthouse** scoring: Aim for 90+ in all categories
- Code splitting and lazy loading for optimal bundle sizes
- Image optimization (WebP, AVIF) with responsive loading
- Minimize JavaScript execution time and main thread blocking

### 2. Accessibility (WCAG 2.1 AA)
- Semantic HTML structure
- ARIA labels and roles where needed
- Keyboard navigation support
- Screen reader compatibility
- Color contrast ratios (4.5:1 for normal text)
- Focus management and visible focus indicators

### 3. Security
- **OWASP Top 10** awareness
- Content Security Policy (CSP) headers
- XSS prevention through proper sanitization
- CSRF token implementation
- Secure authentication flows (OAuth 2.0, JWT best practices)
- Input validation on both client and server

## Framework-Specific Guidelines

### React (Official Docs + Airbnb Style Guide)
```javascript
// Component structure
- Use functional components with hooks
- Keep components small and focused (Single Responsibility)
- Props destructuring for clarity
- PropTypes or TypeScript for type safety
- Avoid inline functions in JSX (performance)
- Use React.memo() for expensive components
- Custom hooks for reusable logic

// State management
- useState for local state
- useReducer for complex state logic
- Context API for theme/auth (avoid prop drilling)
- Redux Toolkit for global app state
- React Query/SWR for server state

// Performance
- useMemo/useCallback to prevent unnecessary re-renders
- Virtualization for long lists (react-window)
- Suspense and lazy() for code splitting
- Error boundaries for graceful error handling
```

### Vue.js (Official Style Guide)
```javascript
// Component organization
- Single File Components (.vue)
- Composition API for Vue 3+
- Props validation with detailed types
- Emit events with descriptive names
- Scoped styles to prevent CSS leakage

// Naming conventions
- PascalCase for component names
- camelCase for props and methods
- kebab-case in templates
- Prefix base components (BaseButton, BaseInput)

// Reactivity
- ref() for primitives
- reactive() for objects
- computed() for derived state
- watch() for side effects
```

### Angular (Official Style Guide)
```typescript
// Architecture
- Feature modules for organization
- Lazy loading routes
- Smart/Dumb component pattern
- Services for business logic
- RxJS for reactive programming

// Best practices
- OnPush change detection strategy
- Unsubscribe from observables (takeUntil pattern)
- Strict TypeScript configuration
- Dependency injection for testability
```

## CSS/Styling Best Practices

### Methodology (BEM + Modern CSS)
```css
/* BEM naming convention */
.block__element--modifier {}

/* CSS Custom Properties for theming */
:root {
  --color-primary: #007bff;
  --spacing-unit: 8px;
}

/* Modern layout */
- CSS Grid for 2D layouts
- Flexbox for 1D layouts
- Container queries for component-level responsiveness
- Logical properties (margin-inline, padding-block)

/* Performance */
- Avoid expensive properties (box-shadow, filter)
- Use transform and opacity for animations
- will-change for optimization hints
- Critical CSS inlining
```

### CSS-in-JS (Styled Components, Emotion)
- Scoped styles by default
- Dynamic styling based on props
- Theme provider for consistent design
- SSR support for performance

### Utility-First (Tailwind CSS)
- Rapid prototyping with utility classes
- PurgeCSS for production optimization
- Custom design system via config
- Component extraction for reusability

## TypeScript Best Practices

```typescript
// Type safety
- Strict mode enabled
- Explicit return types for functions
- Interface over type for objects
- Discriminated unions for state
- Generic types for reusable components
- Avoid 'any' - use 'unknown' instead

// Example
interface User {
  id: string;
  name: string;
  email: string;
}

type Result<T> = 
  | { success: true; data: T }
  | { success: false; error: string };

function fetchUser(id: string): Promise<Result<User>> {
  // Implementation
}
```

## Testing Strategy (Testing Library + Jest)

```javascript
// Testing pyramid
- Unit tests (70%): Pure functions, utilities
- Integration tests (20%): Component interactions
- E2E tests (10%): Critical user flows

// React Testing Library principles
- Test behavior, not implementation
- Query by accessibility attributes
- Avoid testing internal state
- User-centric assertions

// Example
import { render, screen, userEvent } from '@testing-library/react';

test('submits form with user input', async () => {
  render(<LoginForm />);
  
  await userEvent.type(screen.getByLabelText(/email/i), 'user@example.com');
  await userEvent.click(screen.getByRole('button', { name: /submit/i }));
  
  expect(screen.getByText(/success/i)).toBeInTheDocument();
});

// E2E with Playwright/Cypress
- Test critical paths (checkout, signup)
- Visual regression testing
- Cross-browser compatibility
```

## Build & Tooling

### Vite (Modern Build Tool)
- Fast HMR with native ESM
- Optimized production builds
- Plugin ecosystem
- Environment variables (.env files)

### Webpack (Legacy/Complex Projects)
- Code splitting strategies
- Tree shaking for dead code elimination
- Source maps for debugging
- Bundle analysis (webpack-bundle-analyzer)

### Package Management
- Lock files committed (package-lock.json, yarn.lock)
- Audit dependencies regularly (npm audit)
- Semantic versioning understanding
- Monorepo tools (Turborepo, Nx) for large projects

## State Management Patterns

### Local State
- Component state for UI-only concerns
- Form state with libraries (React Hook Form, Formik)

### Global State
```javascript
// Redux Toolkit (recommended)
- Slices for feature-based organization
- createAsyncThunk for API calls
- RTK Query for data fetching
- Immer for immutable updates

// Zustand (lightweight alternative)
- Simple API with hooks
- No boilerplate
- Middleware support

// Jotai/Recoil (atomic state)
- Fine-grained reactivity
- Derived state with selectors
```

## API Integration

```javascript
// Modern data fetching
- React Query / TanStack Query
  - Automatic caching and refetching
  - Optimistic updates
  - Pagination and infinite scroll
  - Mutation handling

// Axios configuration
const api = axios.create({
  baseURL: process.env.VITE_API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptors for auth
api.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login
    }
    return Promise.reject(error);
  }
);
```

## Performance Optimization

### Bundle Size
- Analyze with source-map-explorer or webpack-bundle-analyzer
- Dynamic imports for route-based splitting
- Tree-shakeable imports (import { specific } from 'library')
- Remove unused dependencies

### Runtime Performance
```javascript
// Debouncing and throttling
import { debounce } from 'lodash-es';

const handleSearch = debounce((query) => {
  fetchResults(query);
}, 300);

// Intersection Observer for lazy loading
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      loadComponent(entry.target);
    }
  });
});

// Web Workers for heavy computation
const worker = new Worker('calculation.worker.js');
worker.postMessage({ data: largeDataset });
```

### Caching Strategies
- Service Workers for offline support
- Cache-Control headers
- LocalStorage/IndexedDB for client-side persistence
- Stale-While-Revalidate pattern

## SEO & Meta Tags

```javascript
// React Helmet / Next.js Head
<Helmet>
  <title>Page Title - Site Name</title>
  <meta name="description" content="Page description" />
  <meta property="og:title" content="Social share title" />
  <meta property="og:image" content="https://example.com/image.jpg" />
  <link rel="canonical" href="https://example.com/page" />
</Helmet>

// Structured data (JSON-LD)
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "Article",
  "headline": "Article Title",
  "author": "Author Name"
}
</script>
```

## Deployment & CI/CD

### Environment Configuration
```bash
# .env.production
VITE_API_URL=https://api.production.com
VITE_ANALYTICS_ID=UA-XXXXX-Y

# Never commit secrets
# Use CI/CD environment variables for sensitive data
```

### Build Pipeline
```yaml
# GitHub Actions example
- name: Install dependencies
  run: npm ci
  
- name: Run tests
  run: npm test -- --coverage
  
- name: Build
  run: npm run build
  
- name: Deploy
  run: npm run deploy
```

### Hosting Best Practices
- CDN for static assets (Cloudflare, CloudFront)
- Gzip/Brotli compression
- HTTP/2 or HTTP/3
- SSL/TLS certificates (Let's Encrypt)
- Cache busting with content hashes

## Monitoring & Analytics

```javascript
// Error tracking (Sentry)
Sentry.init({
  dsn: process.env.VITE_SENTRY_DSN,
  environment: process.env.NODE_ENV,
  tracesSampleRate: 0.1,
});

// Performance monitoring
import { getCLS, getFID, getLCP } from 'web-vitals';

getCLS(console.log);
getFID(console.log);
getLCP(console.log);

// User analytics (Google Analytics 4, Plausible)
- Page views
- Custom events
- User flows
- Conversion tracking
```

## Code Quality Tools

### Linting & Formatting
```json
// ESLint configuration
{
  "extends": [
    "eslint:recommended",
    "plugin:react/recommended",
    "plugin:@typescript-eslint/recommended",
    "prettier"
  ],
  "rules": {
    "no-console": "warn",
    "react/prop-types": "off",
    "@typescript-eslint/explicit-module-boundary-types": "off"
  }
}

// Prettier configuration
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5"
}
```

### Git Hooks (Husky + lint-staged)
```json
{
  "husky": {
    "hooks": {
      "pre-commit": "lint-staged"
    }
  },
  "lint-staged": {
    "*.{js,jsx,ts,tsx}": ["eslint --fix", "prettier --write"],
    "*.{css,scss}": ["stylelint --fix", "prettier --write"]
  }
}
```

## Authoritative Sources

### Official Documentation
- [React Docs](https://react.dev) - Official React documentation
- [Vue.js Guide](https://vuejs.org/guide/) - Official Vue.js guide
- [Angular Style Guide](https://angular.io/guide/styleguide) - Official Angular patterns
- [MDN Web Docs](https://developer.mozilla.org) - Web standards reference
- [web.dev](https://web.dev) - Google's web best practices

### Style Guides
- [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript) - Industry standard
- [Google TypeScript Style Guide](https://google.github.io/styleguide/tsguide.html)
- [BEM Methodology](http://getbem.com/) - CSS naming convention

### Performance & Accessibility
- [Web Vitals](https://web.dev/vitals/) - Google's performance metrics
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/) - Accessibility standards
- [Lighthouse](https://developers.google.com/web/tools/lighthouse) - Automated auditing

### Security
- [OWASP Top 10](https://owasp.org/www-project-top-ten/) - Security vulnerabilities
- [Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP) - CSP reference

### Testing
- [Testing Library](https://testing-library.com/) - User-centric testing
- [Jest Documentation](https://jestjs.io/) - JavaScript testing framework
- [Playwright](https://playwright.dev/) - Modern E2E testing

### Tools & Ecosystem
- [Vite Guide](https://vitejs.dev/guide/) - Next-gen build tool
- [TanStack Query](https://tanstack.com/query/latest) - Data fetching library
- [Redux Toolkit](https://redux-toolkit.js.org/) - State management

## Project Structure Example

```
src/
├── assets/          # Static files (images, fonts)
├── components/      # Reusable UI components
│   ├── common/      # Shared components (Button, Input)
│   └── features/    # Feature-specific components
├── hooks/           # Custom React hooks
├── layouts/         # Page layouts
├── pages/           # Route components
├── services/        # API calls and external services
├── store/           # State management (Redux/Zustand)
├── styles/          # Global styles and themes
├── types/           # TypeScript type definitions
├── utils/           # Helper functions
├── App.tsx          # Root component
└── main.tsx         # Entry point
```

## Checklist for Production

- [ ] TypeScript strict mode enabled
- [ ] ESLint and Prettier configured
- [ ] Unit tests with >80% coverage
- [ ] E2E tests for critical flows
- [ ] Lighthouse score >90 in all categories
- [ ] WCAG 2.1 AA compliance verified
- [ ] Security headers configured (CSP, HSTS)
- [ ] Error tracking setup (Sentry)
- [ ] Analytics integrated
- [ ] Environment variables properly configured
- [ ] Bundle size optimized (<200KB initial)
- [ ] Images optimized and lazy loaded
- [ ] SEO meta tags implemented
- [ ] Service worker for offline support (if needed)
- [ ] CI/CD pipeline configured
- [ ] Documentation updated

---

**Last Updated**: 2026-05-16  
**Maintained By**: Development Team  
**Review Cycle**: Quarterly
