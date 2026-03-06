import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

/**
 * Animate element(s) on page load.
 * @param {Ref} targetRef - Vue ref to animate (animates children if animateChildren is true)
 * @param {Object} options - GSAP from() options + { animateChildren: bool }
 */
export function useLoadAnimation(targetRef, options = {}) {
  const { animateChildren = false, ...gsapOptions } = options

  const defaults = {
    y: 20,
    opacity: 0,
    duration: 0.35,
    stagger: 0.08,
    ease: 'power3.out',
  }

  onMounted(() => {
    if (!targetRef.value) return
    const target = animateChildren ? targetRef.value.children : targetRef.value
    gsap.from(target, { ...defaults, ...gsapOptions })
  })
}

/**
 * Animate element(s) when scrolled into view.
 * @param {Ref} targetRef - Vue ref to animate (animates children if animateChildren is true)
 * @param {Object} options - GSAP from() options + { animateChildren: bool, start: string }
 */
export function useScrollAnimation(targetRef, options = {}) {
  const { animateChildren = false, start = 'top 80%', ...gsapOptions } = options

  const defaults = {
    y: 20,
    opacity: 0,
    duration: 0.35,
    stagger: 0.06,
    ease: 'power3.out',
  }

  let triggers = []

  onMounted(() => {
    if (!targetRef.value) return
    const target = animateChildren ? targetRef.value.children : targetRef.value
    gsap.from(target, {
      ...defaults,
      ...gsapOptions,
      scrollTrigger: {
        trigger: targetRef.value,
        start,
        toggleActions: 'play none none none',
      },
    })
    triggers = ScrollTrigger.getAll().slice(-1)
  })

  onUnmounted(() => {
    triggers.forEach((t) => t.kill())
  })
}

/**
 * Animate element with a continuous bounce (e.g. scroll-down indicator).
 * Returns a reactive `visible` ref for use with v-if to hide the element on scroll.
 * @param {Ref} targetRef - Vue ref to animate
 * @param {Object} options - { containerRef, delay, ...gsapFromToOptions }
 * @returns {{ visible: Ref<boolean> }}
 */
export function useBounceAnimation(targetRef, options = {}) {
  const { delay = 1.2, containerRef = null, ...gsapOptions } = options
  const visible = ref(true)
  let bounceTween = null

  const fromDefaults = { y: 0, opacity: 0 }
  const toDefaults = {
    y: 10,
    opacity: 1,
    duration: 1,
    ease: 'power1.inOut',
    repeat: -1,
    yoyo: true,
    delay,
  }

  function initTween() {
    if (targetRef.value) {
      bounceTween = gsap.fromTo(
        targetRef.value,
        { ...fromDefaults },
        { ...toDefaults, ...gsapOptions },
      )
    }
  }

  function handleScroll() {
    if (!containerRef?.value) return
    const rect = containerRef.value.getBoundingClientRect()
    visible.value = rect.bottom > 0
  }

  // Re-init the tween whenever the element is added back to the DOM
  watch(visible, (newVal) => {
    if (newVal) {
      nextTick(() => initTween())
    } else if (bounceTween) {
      bounceTween.kill()
      bounceTween = null
    }
  })

  onMounted(() => {
    initTween()
    if (containerRef) {
      window.addEventListener('scroll', handleScroll, { passive: true })
    }
  })

  onUnmounted(() => {
    if (containerRef) {
      window.removeEventListener('scroll', handleScroll)
    }
    if (bounceTween) {
      bounceTween.kill()
    }
  })

  return { visible }
}
