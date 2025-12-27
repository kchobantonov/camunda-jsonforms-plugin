const USERNAME = 'demo';
const PASSWORD = 'demo';

export const onChange = (event) => {
  let [data] = event.detail;
  console.log('Form state changed:' + JSON.stringify(data));
};

const onLoadRequest = (event) => {
  console.log('onLoadRequest');
  let [requestInfo, requestInit] = event.detail;
  const headers = new Headers(requestInit.headers || {});

  // Basic Auth: base64(username:password)
  const credentials = btoa(`${USERNAME}:${PASSWORD}`);
  headers.set('Authorization', `Basic ${credentials}`);

  requestInit.headers = headers;
};

const onLoadResponse = (event) => {
  console.log('onLoadResponse');
  let [response] = event.detail;
};

const onLoadError = (event) => {
  console.log('onLoadError');
  let [error] = event.detail;

  if (
    error.name === 'AppException' &&
    (error.code === 'RETRIEVE_TASK_DEPLOYED_FORM' ||
      error.code === 'INVALID_TASK_DEPLOYED_FORM_RESPONSE' ||
      error.code === 'RETRIEVE_PROCESS_DEFINITION_DEPLOYED_START_FORM' ||
      error.code === 'INVALID_PROCESS_DEFINITION_DEPLOYED_START_FORM_RESPONSE')
  ) {
    // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
    return;
  }
  if (
    error.name === 'ResponseException' &&
    (error.response.request.url.endsWith('/deployed-start-form') ||
      error.response.request.url.endsWith('/deployed-form'))
  ) {
    // ignore loading from deployed forms - most likely JsonFormsFormServicePlugin was not installed - just log the error in the console
    return;
  }

  alert('Error: ' + error.message);
};

const onSubmitRequest = (event) => {
  console.log('onSubmitRequest');
  let [requestInfo, requestInit] = event.detail;
  const headers = new Headers(requestInit.headers || {});

  // Basic Auth: base64(username:password)
  const credentials = btoa(`${USERNAME}:${PASSWORD}`);
  headers.set('Authorization', `Basic ${credentials}`);

  requestInit.headers = headers;
};

const onSubmitResponse = (event) => {
  console.log('onSubmitResponse');
  let [response] = event.detail;
  if (response.status >= 200 && response.status < 300) {
    alert('Form Completed');
  }
};

const onSubmitError = (event) => {
  console.log('onSubmitError');
  let [error] = event.detail;
  if (error.name === 'AppException' && error.response) {
    let response = error.response;
    if (response.status == 401) {
      alert('Error: You are not authenticated');
      return;
    } else if (response.status == 403) {
      alert('Error: You are not authorized');
      return;
    }
  }

  alert('Error: ' + error.message);
};

export default {
  onChange,
  onLoadRequest,
  onLoadResponse,
  onLoadError,
  onSubmitRequest,
  onSubmitResponse,
  onSubmitError,
};
