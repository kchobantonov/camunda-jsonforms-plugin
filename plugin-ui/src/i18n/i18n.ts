import get from 'lodash/get';

export const createTranslator =
  (locale: string, translations?: Record<string, any>) =>
    (key: string, defaultMessage: string | undefined): string | undefined => {
      if (!translations) return defaultMessage;
      return get(translations[locale], key) ?? defaultMessage;
    };
