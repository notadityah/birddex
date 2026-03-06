<script setup>
import Footer from '@/components/landingpage/FooterComponent.vue'
</script>

<template>
  <main class="grow bg-white/90 backdrop-blur-none">
    <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <h1 class="text-3xl font-bold text-forest-green mb-8">Privacy Policy</h1>

      <div class="space-y-6 text-gray-700 text-sm leading-relaxed">
        <p>Last updated: March 2026</p>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Information We Collect</h2>
          <p>When you create a BirdDex account, we collect the following information:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>
              <strong>Name</strong> &dash; provided during registration or from your Google account
            </li>
            <li>
              <strong>Email address</strong> &dash; used for account identification and
              communication
            </li>
            <li><strong>Account creation date</strong> &dash; recorded automatically</li>
          </ul>
          <p class="mt-2">
            If you sign in with Google, we receive your display name and email address from your
            Google account. We do not access any other Google account data.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">How We Use Your Information</h2>
          <p>Your information is used solely to:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>Create and manage your BirdDex account</li>
            <li>Display your name within the application</li>
            <li>Send password reset emails when requested</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Data Storage & Security</h2>
          <p>
            Your data is stored securely using AWS cloud services, including a managed PostgreSQL
            database and encrypted object storage. Passwords are hashed and never stored in plain
            text. All data is transmitted over encrypted HTTPS connections.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Data Sharing</h2>
          <p>
            We do not sell, trade, or share your personal information with third parties. Your data
            is only used within BirdDex to provide the service.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Your Rights</h2>
          <p>You have the right to:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>Access the personal data we hold about you</li>
            <li>Request correction of inaccurate data</li>
            <li>Request deletion of your account and associated data</li>
          </ul>
          <p class="mt-2">
            To exercise any of these rights, please contact us through the application.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Cookies & Analytics</h2>
          <p>
            BirdDex uses essential cookies for authentication and session management. We do not use
            tracking cookies or third-party analytics.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Changes to This Policy</h2>
          <p>
            We may update this privacy policy from time to time. Any changes will be reflected on
            this page with an updated revision date.
          </p>
        </section>
      </div>
    </div>
  </main>
  <Footer />
</template>
