document.addEventListener('DOMContentLoaded', function() {
  const alerts = document.querySelectorAll('.alert')
  alerts.forEach(function(alert) {
    if (!alert.classList.contains('alert-permanent')) {
      setTimeout(function() {
        alert.classList.add('fade');
        setTimeout(function() {
          alert.remove();
        }, 150);
      }, 5000);
    }
  });

  // Initialize tooltips
  const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
  const tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
    return new bootstrap.Tooltip(tooltipTriggerEl)
  })

  // Initialize copy buttons
  document.querySelectorAll('.copy-button').forEach(button => {
    button.addEventListener('click', function() {
      const content = this.getAttribute('data-content');
      navigator.clipboard.writeText(content).then(() => {
        // Create or update tooltip
        const tooltip = bootstrap.Tooltip.getInstance(this);
        const originalTitle = this.getAttribute('data-bs-original-title');

        // Update tooltip content
        tooltip.setContent({ '.tooltip-inner': 'הועתק!' });

        // Show the updated tooltip
        tooltip.show();

        // Reset tooltip after delay
        setTimeout(() => {
          tooltip.setContent({ '.tooltip-inner': originalTitle });
        }, 1500);
      }).catch(err => {
        console.error('Failed to copy text: ', err);
      });
    });
  });

  // Initialize all upload zones
  document.querySelectorAll('[data-upload-zone]').forEach(zone => {
    initializeDropZone(zone);
  });

  // Initialize file inputs and customize text
  document.querySelectorAll('[data-bs-file-input]').forEach(input => {
    input.addEventListener('change', function(e) {
      handleFileSelect(e);
    });

    // Customize the default text
    customizeFileInputText(input);
  });
});

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
  const modal = button.closest('.modal');
  const viewMode = modal.querySelector('.view-mode');
  const editMode = modal.querySelector('.edit-mode');
  const smartEditMode = modal.querySelector('.smart-edit-mode');
  const editButton = modal.querySelector('.edit-text');
  const smartEditButton = modal.querySelector('.smart-edit-text');

  // Hide all modes first
  viewMode.classList.add('d-none');
  editMode.classList.add('d-none');
  smartEditMode.classList.add('d-none');

  // Reset button texts
  editButton.textContent = 'ערוך';
  smartEditButton.textContent = 'עריכה חכמה';

  // Show selected mode
  switch(mode) {
    case 'view':
      viewMode.classList.remove('d-none');
      break;
    case 'edit':
      editMode.classList.remove('d-none');
      editButton.textContent = 'תצוגה';
      break;
    case 'smart':
      smartEditMode.classList.remove('d-none');
      smartEditButton.textContent = 'תצוגה';
      break;
  }

  function customizeFileInputText(input) {
    const lang = {
      imageInput: {
        default: 'בחירת תמונה',
        noFile: 'לא נבחרה תמונה'
      },
      documentInput: {
        default: 'בחירת קובץ',
        noFile: 'לא נבחר קובץ'
      }
    };

    // Set initial text
    const isImageInput = input.id === 'imageInput';
    input.parentElement.querySelector('.form-control').placeholder =
      isImageInput ? lang.imageInput.noFile : lang.documentInput.noFile;
  }

// Rest of the JavaScript remains the same
  function initializeDropZone(zone) {
    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
      zone.addEventListener(eventName, preventDefaults, false);
    });

    ['dragenter', 'dragover'].forEach(eventName => {
      zone.addEventListener(eventName, () => {
        zone.classList.add('border-primary');
      });
    });

    ['dragleave', 'drop'].forEach(eventName => {
      zone.addEventListener(eventName, () => {
        zone.classList.remove('border-primary');
      });
    });

    zone.addEventListener('drop', handleDrop);
  }

  function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
  }

  function handleDrop(e) {
    const dt = e.dataTransfer;
    const files = dt.files;
    const inputId = this.querySelector('input[type="file"]').id;
    const input = document.getElementById(inputId);

    input.files = files;
    handleFileSelect({ target: input });
  }

  function handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) return;

    const inputId = e.target.id;
    const previewId = inputId.replace('Input', 'Preview');
    const fileNameId = inputId.replace('Input', 'FileName');
    const preview = document.getElementById(previewId);
    const fileNameElement = document.getElementById(fileNameId);

    // Show preview container
    preview.classList.remove('d-none');

    // Update filename
    fileNameElement.textContent = file.name;

    // Handle image preview if it's an image
    if (file.type.startsWith('image/')) {
      const imagePreviewElement = document.getElementById('imagePreviewElement');
      if (imagePreviewElement) {
        const reader = new FileReader();
        reader.onload = function(e) {
          imagePreviewElement.src = e.target.result;
          imagePreviewElement.classList.remove('d-none');
        }
        reader.readAsDataURL(file);
      }
    }
  }

  function clearFileInput(inputId) {
    const input = document.getElementById(inputId);
    const previewId = inputId.replace('Input', 'Preview');
    const preview = document.getElementById(previewId);

    input.value = '';
    preview.classList.add('d-none');

    if (inputId === 'imageInput') {
      const imagePreviewElement = document.getElementById('imagePreviewElement');
      imagePreviewElement.classList.add('d-none');
      imagePreviewElement.src = '';
    }
  }
}

