import { Emitter } from './types';

export interface FetchInterceptorResponse extends Response {
  request: Request;
}

export type RequestInput = [RequestInfo, RequestInit?];

export interface FetchInterceptor {
  request(input: RequestInfo, init?: RequestInit): RequestInput;
  response(response: Response): Response;
  responseError(error: any): any;
}

export class RestClient {
  requestInterceptors: FetchInterceptor[];
  responseInterceptors: FetchInterceptor[];

  constructor(interceptors: FetchInterceptor[]) {
    this.requestInterceptors = [...interceptors];
    this.responseInterceptors = [...interceptors];
    this.responseInterceptors.reverse();
  }

  async fetch(input: RequestInfo, init?: RequestInit): Promise<Response> {
    let args: RequestInput = [input, init ?? {}];

    this.requestInterceptors.forEach((interceptor) => {
      args = interceptor.request(...args);
    });

    const request = new Request(...args);
    try {
      let response = await fetch(request)
        .then((value) => {
          (value as FetchInterceptorResponse).request = request;
          return value;
        })
        .catch((error) => {
          error.request = request;
          return Promise.reject(error);
        });

      this.responseInterceptors.forEach((interceptor) => {
        response = interceptor.response(response);
      });

      return response;
    } catch (e) {
      let error = e;
      this.responseInterceptors.forEach((interceptor) => {
        error = interceptor.responseError(error);
      });

      throw error;
    }
  }
}

export class SubmitEmitter implements FetchInterceptor {
  constructor(private emit: Emitter) {}

  request = (input: RequestInfo, init?: RequestInit): RequestInput => {
    this.emit('submit-request', input, init);
    return [input, init];
  };

  response = (response: Response): Response => {
    this.emit('submit-response', response);
    return response;
  };

  responseError = (error: any): any => {
    this.emit('submit-error', error);
    return error;
  };
}

export class ResponseOkInterceptor implements FetchInterceptor {
  request = (input: RequestInfo, init?: RequestInit): RequestInput => {
    return [input, init];
  };

  response = (response: Response): Response => {
    if (!response.ok) {
      //TODO: remove the throw since we want to show the error message comming from the JSON payload
      //throw new Error(response.statusText);
    }
    return response;
  };
  responseError = (error: any): any => {
    return error;
  };
}
export class LoadEmitter implements FetchInterceptor {
  constructor(private emit: Emitter) {}

  request = (input: RequestInfo, init?: RequestInit): RequestInput => {
    this.emit('load-request', input, init);
    return [input, init];
  };

  response = (response: Response): Response => {
    this.emit('load-response', response);
    return response;
  };
  responseError = (error: any): any => {
    this.emit('load-error', error);
    return error;
  };
}
