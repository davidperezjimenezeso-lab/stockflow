
import { inject } from '@angular/core';
import {
  HttpInterceptorFn
} from '@angular/common/http';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const authService = inject(AuthService);
  const authorization = authService.getAuthorizationHeader();

  // Nunca enviar credenciales a dominios ajenos.
  if (
    !req.url.startsWith('/api/') ||
    !authorization ||
    req.url.endsWith('/auth/me')
  ) {
    return next(req);
  }

  const authenticatedRequest = req.clone({
    setHeaders: {
      Authorization: authorization
    }
  });

  return next(authenticatedRequest);
};
