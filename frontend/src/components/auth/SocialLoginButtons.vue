<script setup></script>

<template>
  <div>
    <!-- Divider -->
    <div class="relative my-6">
      <div class="absolute inset-0 flex items-center">
        <div class="w-full border-t border-gray-200"></div>
      </div>
      <div class="relative flex justify-center text-sm">
        <span class="bg-white px-4 text-gray-500">or continue with</span>
      </div>
    </div>

    <!-- Social Buttons -->
    <div class="flex gap-4">
      <button
        type="button"
        class="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors cursor-pointer"
      >
        <img
          src="/google-icon-logo-svgrepo-com.svg"
          alt=""
          width="20"
          height="20"
          aria-hidden="true"
        />
        Google
      </button>
      <button
        type="button"
        class="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors cursor-pointer"
      >
        <img src="/apple-logo-svgrepo-com.svg" alt="" width="20" height="20" aria-hidden="true" />
        Apple
      </button>
    </div>
  </div>
</template>
