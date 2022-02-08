<script lang="ts">
import { defineComponent } from '@vue/composition-api';

const dataProvider = defineComponent({
  name: 'data-provider',
  props: {
    url: {
      type: String,
      required: true,
    },
  },
  data: () => ({
    data: null as any,
    loaded: false,
    error: undefined as Error,
  }),
  async created() {
    try {
      const response = await fetch(`${this.url}`);
      this.data = await response.json();
    } catch (e) {
      this.error = e;
    } finally {
      this.loaded = true;
    }
  },
  render() {
    return this.$scopedSlots.default({
      fetch: {
        loading: !this.loaded,
        data: this.data,
        error: this.error,
      },
    });
  },
});

export default dataProvider;
</script>
