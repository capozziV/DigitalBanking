/** Methods that write nothing: they need neither the CSRF header nor a JSON body. */
export const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);
