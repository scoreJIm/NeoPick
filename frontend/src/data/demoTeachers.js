export const demoTeachers = [
  {
    id: 'preview-maya', realName: 'Maya Chen', city: { code: 'SH', name: 'Shanghai' }, district: "Jing'an",
    level: 'ADVANCED', bio: 'Touring guitarist and patient teacher for adults who want to play complete songs, not endless drills.',
    basePrice: 188, rating: 4.9, reviewCount: 42, bookingCount: 126,
    tags: ['Indie', 'Electric', 'Songwriting'], categories: ['Electric guitar'], accent: '#ca6b45',
    availability: ['Tomorrow · 19:00', 'Sat · 14:30', 'Sun · 10:00'], isPreview: true,
  },
  {
    id: 'preview-leo', realName: 'Leo Zhang', city: { code: 'NJ', name: 'Nanjing' }, district: 'Xuanwu',
    level: 'INTERMEDIATE', bio: 'Fingerstyle coach focused on technique, rhythm, and helping self-taught players get unstuck.',
    basePrice: 128, rating: 4.8, reviewCount: 31, bookingCount: 89,
    tags: ['Fingerstyle', 'Acoustic', 'Beginner friendly'], categories: ['Acoustic guitar'], accent: '#2f6c68',
    availability: ['Today · 20:00', 'Sat · 09:30', 'Sun · 16:00'], isPreview: true,
  },
  {
    id: 'preview-nora', realName: 'Nora Wu', city: { code: 'HZ', name: 'Hangzhou' }, district: 'Xihu',
    level: 'ADVANCED', bio: 'Classical guitar teacher for children and adults, with structured plans that still leave room for music you love.',
    basePrice: 218, rating: 5, reviewCount: 27, bookingCount: 74,
    tags: ['Classical', 'Kids', 'Music theory'], categories: ['Classical guitar'], accent: '#8b6944',
    availability: ['Wed · 18:30', 'Sat · 11:00', 'Sun · 15:30'], isPreview: true,
  },
]

export function findDemoTeacher(id) {
  return demoTeachers.find((teacher) => teacher.id === id)
}
