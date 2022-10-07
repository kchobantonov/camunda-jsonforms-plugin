# Usage

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Demo</title>
    <!-- include the fonts outside the webcomponent for now - https://github.com/google/material-design-icons/issues/1165 -->
    <style type="text/css">
      @import url("//cdnjs.cloudflare.com/ajax/libs/MaterialDesign-Webfont/6.5.95/css/materialdesignicons.min.css");
    </style>
    <style>
      /* # =================================================================
        # Global selectors
        # ================================================================= */
      html {
        box-sizing: border-box;
        overflow-y: scroll;
        /* All browsers without overlaying scrollbars */
        -webkit-text-size-adjust: 100%;
        /* Prevent adjustments of font size after orientation changes in iOS */
        word-break: normal;
        -moz-tab-size: 4;
        tab-size: 4;
      }
      
      *,
      ::before,
      ::after {
        background-repeat: no-repeat;
        /* Set background-repeat: no-repeat to all elements and pseudo elements */
        box-sizing: inherit;
      }
      
      ::before,
      ::after {
        text-decoration: inherit;
        /* Inherit text-decoration and vertical align to ::before and ::after pseudo elements */
        vertical-align: inherit;
      }
      
      * {
        padding: 0;
        /* Reset padding and margin of all elements */
        margin: 0;
      }
    </style>
  </head>
  <body>
    <script type="text/javascript">
      const data = {"firstName":"Test"};
      const schema = {"type":"object","required":["firstName"],"properties":{"address":{"type":"string"},"gender":{"type":"string","enum":["Male","Female","Undisclosed"]},"firstName":{"type":"string","minLength":2,"maxLength":20},"lastName":{"type":"string","minLength":5,"maxLength":15}}};
      const uischema = {"type":"VerticalLayout","elements":[{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/firstName"},{"type":"Control","scope":"#/properties/lastName"}]},{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/gender"}]},{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/properties/address"}]}]};
      const config = {"restrict":true,"trim":false,"showUnfocusedDescription":false,"hideRequiredAsterisk":true};
      const preset = {"theme":{"dark":false}}
      const onChange = (event) => {
        let [data] = event.detail;
        console.log('Form state changed:' + JSON.stringify(data));
      };
    </script>

    <vuetify-json-forms id="vuetify-json-forms">
    </vuetify-json-forms>

    <script>
      let form = document.getElementById('vuetify-json-forms');
      form.setAttribute('custom-style', '.v-application--wrap { min-height: 0px; }');
      form.setAttribute('data', JSON.stringify(data));
      form.setAttribute('schema', JSON.stringify(schema));
      form.setAttribute('uischema', JSON.stringify(uischema));
      form.setAttribute('config', JSON.stringify(config));
      form.setAttribute('default-preset', JSON.stringify(preset));
      form.addEventListener('change', onChange);
    </script>
    <script type="text/javascript" src="vuetify-json-forms.min.js"></script>
  </body>
</html>
```

Using the above page and `vuetify-json-forms.min.js` you can render the provider initial data with the `data` attribute and upon changes of the data you can listen for those in `onChange` method
