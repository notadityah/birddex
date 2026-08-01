// The three-step explainer, shared by the public landing page (HowItWorks)
// and the first-run WelcomeModal so the two never drift apart.
export const onboardingSteps = [
  {
    number: '01',
    iconSrc: '/camera-svgrepo-com.svg',
    iconAlt: 'Camera',
    title: 'Snap a Photo',
    description:
      'Take a picture of any bird you spot — in your backyard, at the park, or anywhere outdoors.',
  },
  {
    number: '02',
    iconSrc: '/bulb-on-svgrepo-com.svg',
    iconAlt: 'Identify',
    title: 'Get Instant ID',
    description:
      'Our bird detection model analyses your photo and identifies the bird species in seconds.',
  },
  {
    number: '03',
    iconSrc: '/collection-data-database-document-svgrepo-com.svg',
    iconAlt: 'Collection',
    title: 'Build Your Collection',
    description:
      'Every identified bird gets added to your personal collection. Track your progress and share sightings.',
  },
]
