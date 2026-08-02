<script setup>
import Footer from '@/components/landingpage/FooterComponent.vue'
</script>

<template>
  <main class="grow bg-white/90 backdrop-blur-none">
    <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <h1 class="text-3xl font-bold text-forest-green mb-8">Privacy Policy</h1>

      <div class="space-y-6 text-gray-700 text-sm leading-relaxed">
        <p>Last updated: August 2026</p>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Information We Collect</h2>
          <p>When you create a BirdDex account, we collect:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>
              <strong>Name</strong> - provided during registration or from your Google account
            </li>
            <li>
              <strong>Email address</strong> - used for account identification and communication
            </li>
            <li><strong>Account creation date</strong> - recorded automatically</li>
            <li>
              <strong>Profile picture URL</strong> - only if you sign in with Google
            </li>
          </ul>

          <p class="mt-3">As you use BirdDex, we also store:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>
              <strong>Bird photos you upload</strong> - kept in encrypted cloud storage. Photos are
              resized in your browser before upload, which removes camera metadata such as GPS
              coordinates.
            </li>
            <li>
              <strong>Sightings</strong> - the identified species, the date, and any notes you add
            </li>
            <li>
              <strong>Feedback you submit</strong> - the category, your message, the page you sent it
              from, and an optional photo
            </li>
          </ul>

          <p class="mt-3">And we record automatically, for security and account management:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>
              <strong>IP address and browser user agent</strong> - stored with each sign-in session
            </li>
            <li>
              <strong>Google sign-in tokens</strong> - if you use Google, the access and refresh
              tokens that keep you signed in
            </li>
            <li>
              <strong>Moderation records</strong> - if we ever need to restrict an account, the
              reason and any internal notes
            </li>
          </ul>

          <p class="mt-3">
            If you sign in with Google, we receive your display name, email address and profile
            picture from your Google account. We do not access any other Google account data.
          </p>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">How We Use Your Information</h2>
          <p>Your information is used solely to:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>Create and manage your BirdDex account</li>
            <li>Display your name within the application</li>
            <li>Send account emails, such as email verification and password resets</li>
            <li>Identify birds in the photos you upload and record your sightings</li>
            <li>Review and act on the feedback you send us</li>
            <li>Keep your account secure and investigate misuse</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Data Storage & Security</h2>
          <p>
            Your account details, sightings and feedback are stored in a managed PostgreSQL database
            hosted by Neon. Your uploaded photos are stored in encrypted object storage on Amazon Web
            Services (AWS), which also hosts the application. Both are located in the Asia Pacific
            (Sydney) region. Passwords are hashed and never stored in plain text, and all data is
            transmitted over encrypted HTTPS connections.
          </p>
          <p class="mt-2">How long we keep things:</p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li>Sign-in sessions expire after 7 days</li>
            <li>Server logs are retained for 14 days</li>
            <li>
              Deleted photos may persist in backup storage for up to 30 days before being purged
            </li>
            <li>
              Your account, sightings and photos are kept until you ask us to delete them
            </li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-semibold text-forest-green mb-2">Data Sharing</h2>
          <p>
            We do not sell or trade your personal information. We rely on a small number of service
            providers to operate BirdDex, and they process your data only for that purpose:
          </p>
          <ul class="list-disc pl-6 mt-2 space-y-1">
            <li><strong>Amazon Web Services</strong> - application hosting and photo storage</li>
            <li><strong>Neon</strong> - database hosting</li>
            <li><strong>Resend</strong> - sending account emails (receives your email address)</li>
            <li><strong>Google</strong> - sign-in, only if you choose to use it</li>
          </ul>
          <p class="mt-3">
            Separately, if you choose to share a sighting to the Community Gallery, that photo, the
            species, the date and any notes you added become
            <strong>visible to every signed-in BirdDex user</strong>. The gallery is not open to the
            public web, but it is not private to you either. Enabling "Appear as Anonymous in
            gallery" in My Account hides your display name only - the photo, date and notes are still
            shown. You can unshare a sighting at any time from MyDex.
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
            You can delete all of your own sightings and photos at any time using
            <strong>Reset Collection</strong> in My Account. To delete your account entirely, or to
            exercise any of the other rights above, send us a request using the feedback button in
            the app and we will action it and confirm once it is done.
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
