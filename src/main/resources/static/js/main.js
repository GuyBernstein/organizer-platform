document.addEventListener('DOMContentLoaded', function() {
  const alerts = document.querySelectorAll('.alert')
  alerts.forEach(function(alert) {
    if (!alert.classList.contains('alert-permanent')) {
      setTimeout(function() {
        alert.classList.add('fade')
        setTimeout(function() {
          alert.remove()
        }, 150)
      }, 5000)
    }
  })

  // Initialize tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  tooltipTriggerList.map(function(tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })
// Initialize copy buttons
  document.querySelectorAll('.copy-button').forEach(button => {
    button.addEventListener('click', function() {
      const content = this.getAttribute('data-content')
      navigator.clipboard.writeText(content).then(() => {
        // Create or update tooltip
        const tooltip = bootstrap.Tooltip.getInstance(this)
        const originalTitle = this.getAttribute('data-bs-original-title')

        // Update tooltip content
        tooltip.setContent({ '.tooltip-inner': 'הועתק!' })

        // Show the updated tooltip
        tooltip.show()

        // Reset tooltip after delay
        setTimeout(() => {
          tooltip.setContent({ '.tooltip-inner': originalTitle })
        }, 1500)
      }).catch(err => {
        console.error('Failed to copy text: ', err)
      })
    })
  })

  // Initialize all upload zones
  document.querySelectorAll('[data-upload-zone]').forEach(zone => {
    initializeDropZone(zone)
  })

  // Initialize file inputs
  document.querySelectorAll('input[type="file"]').forEach(input => {
    input.addEventListener('change', handleFileSelect)
    customizeFileInputText(input)
  })


  // Get the canvas element
  const ctx = document.getElementById('categoriesChart').getContext('2d');

  // Get data from Thymeleaf
  const categoriesData = /*[[${categoriesDistribution}]]*/ [];

  // Prepare the data for Chart.js
  const labels = categoriesData.map(item => item.name);
  const data = categoriesData.map(item => item.count);
  const backgroundColors = [
    '#128C7E',  // WhatsApp teal
    '#25D366',  // WhatsApp green
    '#075E54',  // WhatsApp dark
    '#34B7F1',  // WhatsApp light blue
    // Add more colors if needed
  ];

  // Create the chart
  new Chart(ctx, {
    type: 'bar',  // You can change this to 'pie', 'doughnut', etc.
    data: {
      labels: labels,
      datasets: [{
        label: 'מספר הודעות לפי קטגוריה',
        data: data,
        backgroundColor: backgroundColors,
        borderColor: 'rgba(255, 255, 255, 0.8)',
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top',
          rtl: true,
          labels: {
            font: {
              size: 14
            }
          }
        },
        title: {
          display: false
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          position: 'right',  // For RTL support
          ticks: {
            font: {
              size: 12
            }
          }
        },
        x: {
          ticks: {
            font: {
              size: 12
            }
          }
        }
      }
    }
  });
})

// Keep track of loaded messages per subcategory
const loadedMessages = {}

function loadMoreMessages(categoryKey, subcategoryKey) {
  const key = `${categoryKey}-${subcategoryKey}`
  const offset = (loadedMessages[key] || 0) + 50

  // Make AJAX call to load more messages
  fetch(`/api/messages?category=${categoryKey}&subcategory=${subcategoryKey}&offset=${offset}&limit=50`)
    .then(response => response.json())
    .then(data => {
      loadedMessages[key] = offset
      // Append new messages to the container
      const container = document.querySelector(`[data-category="${categoryKey}"][data-subcategory="${subcategoryKey}"] .messages-container`)
      // Append new message buttons...
    })
}

// intersection observer for infinite scrolling
const observerOptions = {
  root: null,
  rootMargin: '20px',
  threshold: 0.1
}

const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const container = entry.target
      const categoryKey = container.dataset.category
      const subcategoryKey = container.dataset.subcategory
      loadMoreMessages(categoryKey, subcategoryKey)
    }
  })
}, observerOptions)

// Observe all message containers
document.querySelectorAll('.messages-container').forEach(container => {
  observer.observe(container)
})

// toggle between view, edit and smart edit mode in the modal
function toggleMode(button, mode) {
  const modal = button.closest('.modal')
  const viewMode = modal.querySelector('.view-mode')
  const editMode = modal.querySelector('.edit-mode')
  const smartEditMode = modal.querySelector('.smart-edit-mode')
  const editButton = modal.querySelector('.edit-text')
  const smartEditButton = modal.querySelector('.smart-edit-text')

  // Hide all modes first
  viewMode.classList.add('d-none')
  editMode.classList.add('d-none')
  smartEditMode.classList.add('d-none')

  // Reset button texts
  editButton.textContent = 'ערוך'
  smartEditButton.textContent = 'עריכה חכמה'

  // Show selected mode
  switch (mode) {
    case 'view':
      viewMode.classList.remove('d-none')
      break
    case 'edit':
      editMode.classList.remove('d-none')
      editButton.textContent = 'תצוגה'
      break
    case 'smart':
      smartEditMode.classList.remove('d-none')
      smartEditButton.textContent = 'תצוגה'
      break
  }
}

function initializeDropZone(zone) {
  const input = zone.querySelector('input[type="file"]')
  if (!input) return;

  // Prevent defaults for all drag events
  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, preventDefaults, false)
  });

  // Add highlighting when dragging over
  ['dragenter', 'dragover'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.add('border-primary')
      zone.classList.add('border')
    })
  });

  // Remove highlighting when leaving or dropping
  ['dragleave', 'drop'].forEach(eventName => {
    zone.addEventListener(eventName, () => {
      zone.classList.remove('border-primary')
      zone.classList.remove('border')
    })
  })

  // Handle the actual drop
  zone.addEventListener('drop', (e) => {
    const dt = e.dataTransfer
    const files = dt.files

    // Update the input's files
    const dataTransfer = new DataTransfer()
    Array.from(files).forEach(file => dataTransfer.items.add(file))
    input.files = dataTransfer.files

    // Trigger the file select handler
    handleFileSelect({ target: input })
  })
}

function handleFileSelect(e) {
  const input = e.target
  const file = input.files[0]
  if (!file) return

  const dropZone = input.closest('[data-upload-zone]')
  const isImageInput = input.accept.includes('image')

  // Find preview elements within the specific drop zone
  const preview = dropZone.querySelector('.file-preview')
  const imagePreview = dropZone.querySelector('.image-preview')
  const fileName = dropZone.querySelector('.file-name')

  // Update filename if element exists
  if (fileName) {
    fileName.textContent = file.name
  }

  // Handle image preview for image files
  if (isImageInput && file.type.startsWith('image/') && imagePreview) {
    const reader = new FileReader()
    reader.onload = function(e) {
      imagePreview.src = e.target.result
      imagePreview.classList.remove('d-none')
    }
    reader.readAsDataURL(file)
  }

  // Show preview container if it exists
  if (preview) {
    preview.classList.remove('d-none')
  }
}

function preventDefaults(e) {
  e.preventDefault()
  e.stopPropagation()
}

function customizeFileInputText(input) {
  const label = input.closest('[data-upload-zone]').querySelector('label')
  if (!label) return

  const isImageInput = input.accept.includes('image')
  label.textContent = isImageInput ? 'בחירת תמונה' : 'בחירת קובץ'
}

function clearFileInput(inputId) {
  const input = document.getElementById(inputId)
  if (!input) return

  const dropZone = input.closest('[data-upload-zone]')
  if (!dropZone) return

  // Clear the input
  input.value = ''

  // Reset preview elements
  const preview = dropZone.querySelector('.file-preview')
  const imagePreview = dropZone.querySelector('.image-preview')
  const fileName = dropZone.querySelector('.file-name')

  if (preview) preview.classList.add('d-none')
  if (imagePreview) {
    imagePreview.classList.add('d-none')
    imagePreview.src = ''
  }
  if (fileName) fileName.textContent = ''
}



