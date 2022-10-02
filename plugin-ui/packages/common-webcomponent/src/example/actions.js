const showRequiredAsterisk = (event) => {
  event.jsonforms.config.hideRequiredAsterisk = false;
};

const hideRequiredAsterisk = (event) => {
  event.jsonforms.config.hideRequiredAsterisk = true;
};

const showData = (event) => {
  const root = event.$el.getRootNode();
  root.querySelector('#data').style.display = 'block';
};

const hideData = (event) => {
  const root = event.$el.getRootNode();
  root.querySelector('#data').style.display = 'none';
};

const saveData = async (event) => {
  // use sleep function to simulate the REST call
  function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  const data = event.jsonforms.core.data;
  if (event.jsonforms.core.errors && event.jsonforms.core.errors.length > 0) {
    const uidata = event.context.uidata;
    uidata.errorDialog.title = event.jsonforms.i18n.translate('Error', 'Error');
    uidata.errorDialog.text = event.jsonforms.i18n.translate(
      'You have errors. Fix those before saving data.',
      'You have errors. Fix those before saving data.'
    );
    uidata.errorDialog.show = true;
  } else {
    try {
      event.jsonforms.readonly = true;
      await sleep(4000);

      alert('Data Saved:' + JSON.stringify(data));
    } finally {
      event.jsonforms.readonly = false;
    }
  }
};

const changeLangToEn = (event) => {
  let form = event.$el.getRootNode().host;

  form.setAttribute('locale', 'en');
};

const changeLangToBg = (event) => {
  let form = event.$el.getRootNode().host;

  form.setAttribute('locale', 'bg');
};

let actions = {
  hideRequiredAsterisk: hideRequiredAsterisk,
  showRequiredAsterisk: showRequiredAsterisk,
  showData: showData,
  hideData: hideData,
  saveData: saveData,
  changeLangToEn: changeLangToEn,
  changeLangToBg: changeLangToBg,
};

export const onInit = (customEvent) => {
  let [event] = customEvent.detail;

  if (event && event.registerActions) {
    event.registerActions(actions);
  }
};

export const onChange = (event) => {
  let [data] = event.detail;
  console.log('Form state changed:' + JSON.stringify(data));
};
