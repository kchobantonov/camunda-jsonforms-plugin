// load the camunda-json-forms via script tag injection to allow the bootstrap to propertly setup the public path to allow proper loading of jsonform js chunks

(function () {
  "use strict";
  var src = "../scripts/jsonforms/camunda-json-forms.js";
  var el = document.querySelector('script[src="' + src + '"]');
  if (!el) {
    el = document.createElement("script");
    el.type = "module";
    el.src = src;

    document.head.appendChild(el);
  }
})();
