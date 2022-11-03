// load the camunda-json-forms via script tag injection to allow the bootstrap to propertly setup the public path to allow proper loading of jsonform js chunks
const jsonforms = document.createElement('script');
jsonforms.src = '../scripts/jsonforms/camunda-json-forms.min.js';
document.body.append(jsonforms);
