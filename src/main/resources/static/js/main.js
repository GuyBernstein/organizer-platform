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

  // Access the data through the global variable
  if (window.categoriesHierarchyData) {
    initializeCategoriesChart(window.categoriesHierarchyData)
  }

  // Initialize the chart when the document is ready
  if(window.miniCategoriesData && window.treeMapOptions){
    initializeTreeMap(window.miniCategoriesData, window.treeMapOptions)
  }

  initializeUsersChart(window.authorizedUsers , window.adminUsers , window.unauthorizedUsers)

  initUserActivityChart(window.userCountsByDate, window.cumulativeCountsByDate)
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

function initializeCategoriesChart(hierarchyData) {
  if (document.getElementById('categoriesChart') === null)
    return
  const ctx = document.getElementById('categoriesChart').getContext('2d')

  // Transform the nested map structure into the required tree format
  const transformedData = [];
  Object.entries(hierarchyData).forEach(([category, subCategories]) => {
    Object.entries(subCategories).forEach(([subCategory, count]) => {
      transformedData.push({
        category: category,
        subCategory: subCategory,
        value: count
      });
    });
  });

  // Generate a color palette for a category
  const generateCategoryColors = (() => {
    const baseColors = [
      { r: 78, g: 121, b: 167 },  // Blue
      { r: 242, g: 142, b: 43 },  // Orange
      { r: 89, g: 161, b: 79 },   // Green
      { r: 237, g: 201, b: 72 },  // Yellow
      { r: 225, g: 87, b: 89 },   // Red
      { r: 130, g: 183, b: 180 }, // Teal
      { r: 176, g: 122, b: 161 }, // Purple
      { r: 255, g: 157, b: 167 }, // Pink
      { r: 156, g: 117, b: 95 }   // Brown
    ];

    let colorIndex = 0;
    const categoryColorMap = new Map();

    return (category) => {
      if (!categoryColorMap.has(category)) {
        const baseColor = baseColors[colorIndex % baseColors.length];
        const colors = [
          `rgb(${baseColor.r}, ${baseColor.g}, ${baseColor.b})`,
          `rgb(${Math.min(baseColor.r + 30, 255)}, ${Math.min(baseColor.g + 30, 255)}, ${Math.min(baseColor.b + 30, 255)})`
        ];
        categoryColorMap.set(category, colors);
        colorIndex++;
      }
      return categoryColorMap.get(category);
    };
  })();

  // Create the chart
  new Chart(ctx, {
    type: 'treemap',
    data: {
      datasets: [{
        tree: transformedData,
        key: 'value',
        groups: ['category', 'subCategory'],
        spacing: 1,
        borderWidth: 1.5,
        borderColor: 'white',
        backgroundColor: function(ctx) {
          if (!ctx.raw) return 'gray';
          const category = ctx.raw._data.category || 'לא מזוהה';
          const palette = generateCategoryColors(category);
          return palette[ctx.dataIndex % palette.length];
        },
        labels: {
          display: true,
          align: 'center',
          position: 'center',
          formatter: function(ctx) {
            if (!ctx.raw) return 'לא מזוהה';
            const subCategory = ctx.raw._data.subCategory;
            const value = ctx.raw._data.value;
            return `${subCategory}\n(${value})`;
          },
          font: {
            family: "'Heebo', sans-serif",
            size: 13
          },
          color: function(ctx) {
            const backgroundColor = ctx.dataset.backgroundColor(ctx);
            const rgb = backgroundColor.match(/\d+/g);
            if (!rgb) return '#000000';
            const brightness = (parseInt(rgb[0]) * 299 + parseInt(rgb[1]) * 587 + parseInt(rgb[2]) * 114) / 1000;
            return brightness > 128 ? '#000000' : '#FFFFFF';
          }
        }
      }]
    },
    options: {
      plugins: {
        title: {
          display: true,
          text: 'התפלגות קטגוריות ותת קטגוריות לכמות הודעות',
          font: {
            size: 16,
            weight: 'bold'
          }
        },
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            title: function(context) {
              const item = context[0].raw;
              if (!item) return 'לא מזוהה';
              return `${item._data.category}`;
            },
            label: function(context) {
              return `כמות: ${context.raw._data.value}`;
            }
          }
        }
      }
    }
  });
}

// Function to initialize the treemap
function initializeTreeMap(data, options) {
  if (document.getElementById('treeMapChart') === null)
    return
  const ctx = document.getElementById('treeMapChart').getContext('2d');
  new Chart(ctx, {
    type: 'treemap',
    data: {
      datasets: [{
        tree: data,
        key: 'value',
        groups: ['name'],
        spacing: 0.5,
        borderWidth: 1,
        borderColor: 'white',
        backgroundColor: function(ctx) {
          // Generate colors based on the category
          const colors = ['#FF9999', '#99FF99', '#9999FF', '#FFFF99'];
          return colors[ctx.dataIndex % colors.length];
        },
        labels: {
          display: true,
          align: 'center',
          position: 'center',
          font: {
            family: "'Heebo', sans-serif",
            size: 14
          }
        }
      }]
    },
    options: options
  });
}

function initializeUsersChart(authorizedUsers , adminUsers , unauthorizedUsers){
  if (document.getElementById('authorizationChart') === null)
    return

  // Calculate total for percentages
  const total = authorizedUsers + adminUsers + unauthorizedUsers;

  // Get the canvas element
  const ctx = document.getElementById('authorizationChart').getContext('2d');

  // Create the pie chart
  new Chart(ctx, {
    type: 'pie',
    data: {
      labels: ['משתמשים מורשים', 'משתמשי מנהל', 'משתמשים לא מורשים'],
      datasets: [{
        data: [authorizedUsers, adminUsers, unauthorizedUsers],
        backgroundColor: [
          '#198754',  // bg-success color for authorized users
          '#0dcaf0',  // bg-info color for admin users
          '#212529'   // bg-dark color for unauthorized users
        ],
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            font: {
              size: 14
            }
          }
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const value = context.raw;
              const percentage = ((value / total) * 100).toFixed(1);
              return `${context.label}: ${value} (${percentage}%)`;
            }
          }
        },
        datalabels: {
          color: '#fff',
          font: {
            weight: 'bold',
            size: 18
          },
          formatter: function(value, context) {
            const percentage = ((value / total) * 100).toFixed(1);
            // Only show label if percentage is greater than 0
            if (percentage > 0) {
              return `${value}\n(${percentage}%)`;
            } else {
              return null;  // This will hide the label
            }
          },
          anchor: 'center',
          align: 'right',   // This aligns the label to the right of the anchor point
          offset: 20,       // This moves the label 20 pixels to the right from the anchor point
          padding: {
            left: 0,
            right: 0
          }
        }
      },
      layout: {
        padding: {
          top: 10,
          bottom: 10
        }
      }
    },
    plugins: [ChartDataLabels]
  });
}


// Function to initialize user activity chart
function initUserActivityChart(datesGlobal, countsGlobal) {
  if (document.getElementById('usersActivityChart') === null)
    return

  const ctx = document.getElementById('usersActivityChart')

  const dates = Object.keys(datesGlobal)
  const counts = Object.values(countsGlobal)

  new Chart(ctx, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [{
        label: 'סה״כ משתמשים',
        data: counts,
        borderColor: '#25D366',
        backgroundColor: 'rgba(37, 211, 102, 0.1)',
        fill: true
      }]
    },
    options: {
      responsive: true,
      scales: {
        x: {
          type: 'time',
          time: {
            unit: 'day',
            displayFormats: {
              day: 'yyyy-MM-dd'
            }
          },
          title: {
            display: true,
            text: 'תאריך',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: 'מספר משתמשים',
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        }
      },
      plugins: {
        legend: {
          labels: {
            font: {
              size: 16,
              weight: 'bold'
            }
          }
        },
        tooltip: {
          titleFont: {
            size: 14
          },
          bodyFont: {
            size: 16
          },
          callbacks: {
            label: function(context) {
              const date = context.label
              const totalUsers = context.parsed.y
              const dailyUsers = window.userCountsByDate[date] || 0
              return [
                `סה״כ משתמשים: ${totalUsers}`,
                `משתמשים חדשים: ${dailyUsers}`
              ]
            }
          }
        }
      }
    }
  })
}

