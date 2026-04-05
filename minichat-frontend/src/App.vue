<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <keep-alive include="Friend">
        <component :is="Component" />
      </keep-alive>
    </router-view>
  </div>
</template>


<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100vh;
}
</style>
