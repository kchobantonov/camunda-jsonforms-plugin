import get from 'lodash/get';

export const createTranslator =
  (locale: string, translations?: Record<string, any>) =>
  (key: string, defaultMessage: string | undefined): string | undefined => {
    if (!translations) return defaultMessage;

    let localeTraslations = translations[locale];
    if (!localeTraslations) {
      const dashIndex = locale.indexOf('-');
      localeTraslations =
        dashIndex > 0
          ? translations[locale.substring(0, dashIndex)]
          : translations['en'];
    }
    return get(localeTraslations, key) ?? defaultMessage;
  };
